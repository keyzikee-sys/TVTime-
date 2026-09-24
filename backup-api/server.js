import http from "node:http";
import { neon } from "@neondatabase/serverless";
import { OAuth2Client } from "google-auth-library";

const sql = neon(process.env.DATABASE_URL);

const GOOGLE_CLIENT_ID =
  process.env.GOOGLE_CLIENT_ID ||
  "76963040426-e5ap5fhq3qnhm8mistlmmlnkeee4ureg.apps.googleusercontent.com";

const googleClient = new OAuth2Client(GOOGLE_CLIENT_ID);

function sendJson(res, status, data) {
  const body = JSON.stringify(data);

  res.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
  });

  res.end(body);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";

    req.setEncoding("utf8");

    req.on("data", chunk => {
      body += chunk;

      if (body.length > 1000000) {
        reject(new Error("Request body too large"));
        req.destroy();
      }
    });

    req.on("end", () => resolve(body));
    req.on("error", reject);
  });
}

async function verifyGoogleToken(token) {
  const ticket = await googleClient.verifyIdToken({
    idToken: token,
    audience: GOOGLE_CLIENT_ID
  });

  return ticket.getPayload();
}

async function getGoogleUser(req) {
  const auth = req.headers.authorization || "";

  if (!auth.startsWith("Bearer ")) {
    throw new Error("Missing Google ID token");
  }

  const token = auth.substring(7).trim();

  if (!token) {
    throw new Error("Missing Google ID token");
  }

  const payload = await verifyGoogleToken(token);

  if (!payload?.sub) {
    throw new Error("Google account ID missing");
  }

  return payload;
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
        "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
      });
      res.end();
      return;
    }

    if (req.method === "GET" && req.url === "/health") {
      sendJson(res, 200, {
        success: true,
        message: "TewbyTime Backup API is running"
      });
      return;
    }

    if (req.method === "POST" && req.url === "/backup") {
      const googleUser = await getGoogleUser(req);
      const body = await readBody(req);

      let data;

      try {
        data = JSON.parse(body);
      } catch {
        sendJson(res, 400, {
          success: false,
          message: "Invalid JSON"
        });
        return;
      }

      if (!data || typeof data.backupData !== "object") {
        sendJson(res, 400, {
          success: false,
          message: "backupData is required"
        });
        return;
      }

      const schemaVersion =
        Number.isInteger(data.schemaVersion)
          ? data.schemaVersion
          : 1;

      await sql`
        INSERT INTO tewbytime_backups
          (google_sub, backup_data, schema_version)
        VALUES
          (${googleUser.sub}, ${JSON.stringify(data.backupData)}::jsonb, ${schemaVersion})
        ON CONFLICT (google_sub)
        DO UPDATE SET
          backup_data = EXCLUDED.backup_data,
          schema_version = EXCLUDED.schema_version,
          updated_at = NOW()
      `;

      sendJson(res, 200, {
        success: true,
        message: "Backup saved"
      });

      return;
    }

    if (req.method === "GET" && req.url === "/backup") {
      const googleUser = await getGoogleUser(req);

      const rows = await sql`
        SELECT backup_data, schema_version, updated_at
        FROM tewbytime_backups
        WHERE google_sub = ${googleUser.sub}
        LIMIT 1
      `;

      if (rows.length === 0) {
        sendJson(res, 404, {
          success: false,
          message: "No backup found"
        });
        return;
      }

      sendJson(res, 200, {
        success: true,
        backupData: rows[0].backup_data,
        schemaVersion: rows[0].schema_version,
        updatedAt: rows[0].updated_at
      });

      return;
    }

    sendJson(res, 404, {
      success: false,
      message: "Not found"
    });
  } catch (error) {
    console.error("Backup API error:", error);

    sendJson(res, 401, {
      success: false,
      message: error.message || "Unauthorized"
    });
  }
});

const PORT = Number(process.env.PORT) || 3000;

server.listen(PORT, "0.0.0.0", () => {
  console.log(`TewbyTime Backup API listening on port ${PORT}`);
});
