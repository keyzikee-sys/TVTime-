import http from "node:http";
import { neon } from "@neondatabase/serverless";

const sql = neon(process.env.DATABASE_URL);

export async function register(code, deviceId) {
  if (!code || !deviceId) {
    return {
      success: false,
      status: 400,
      message: "Registration code and device ID are required"
    };
  }

  const rows = await sql`
    SELECT id, code, device_id, active, revoked_at
    FROM registration_codes
    WHERE code = ${code}
    LIMIT 1
  `;

  if (rows.length === 0) {
    return {
      success: false,
      status: 401,
      message: "Invalid registration code"
    };
  }

  const registration = rows[0];

  if (!registration.active || registration.revoked_at !== null) {
    return {
      success: false,
      status: 403,
      message: "Registration code is inactive"
    };
  }

  if (
    registration.device_id !== null &&
    registration.device_id !== deviceId
  ) {
    return {
      success: false,
      status: 409,
      message: "Registration code is already bound to another device"
    };
  }

  if (registration.device_id === null) {
    await sql`
      UPDATE registration_codes
      SET device_id = ${deviceId},
          bound_at = NOW()
      WHERE id = ${registration.id}
    `;
  }

  return {
    success: true,
    status: 200,
    message: "REGISTERED"
  };
}

function sendJson(res, status, data) {
  const body = JSON.stringify(data);

  res.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(body),
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Allow-Methods": "POST, OPTIONS"
  });

  res.end(body);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";

    req.setEncoding("utf8");

    req.on("data", chunk => {
      body += chunk;

      if (body.length > 10000) {
        reject(new Error("Request body too large"));
        req.destroy();
      }
    });

    req.on("end", () => resolve(body));
    req.on("error", reject);
  });
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "Content-Type",
        "Access-Control-Allow-Methods": "POST, OPTIONS"
      });
      res.end();
      return;
    }

    if (req.method === "GET" && req.url === "/health") {
      sendJson(res, 200, {
        success: true,
        message: "TewbyTime Registration API is running"
      });
      return;
    }

    if (req.method === "POST" && req.url === "/register") {
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

      const code =
        typeof data.code === "string"
          ? data.code.trim()
          : "";

      const deviceId =
        typeof data.deviceId === "string"
          ? data.deviceId.trim()
          : "";

      const result = await register(code, deviceId);

      sendJson(res, result.status, {
        success: result.success,
        message: result.message
      });

      return;
    }

    sendJson(res, 404, {
      success: false,
      message: "Not found"
    });
  } catch (error) {
    console.error("API error:", error);

    sendJson(res, 500, {
      success: false,
      message: "Internal server error"
    });
  }
});

const PORT = Number(process.env.PORT) || 3000;

server.listen(PORT, "0.0.0.0", () => {
  console.log(`TewbyTime Registration API listening on port ${PORT}`);
});
