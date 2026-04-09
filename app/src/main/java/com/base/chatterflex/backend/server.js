import express from "express";
import fetch from "node-fetch";
import cors from "cors";
import dotenv from "dotenv";

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

let latestMessage = "Hello from Chatterflex";

// ESP32 sends gesture
app.post("/send", (req, res) => {
  const { message } = req.body;
  if (message) {
    latestMessage = message;
    console.log("ESP32:", message);
  }
  res.json({ status: "ok" });
});

// Android fetches AI response
app.get("/getmessage", async (req, res) => {
  try {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${process.env.GEMINI_API_KEY}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text: `You are ChatterFlex assistant.\nUser: ${latestMessage}`,
                },
              ],
            },
          ],
        }),
      }
    );

    const data = await response.json();

    const text =
      data.candidates?.[0]?.content?.parts?.[0]?.text ||
      "No response";

    res.json({ message: text });

  } catch (err) {
    res.json({ message: "Error: " + err.message });
  }
});

app.get("/", (req, res) => {
  res.send("Backend running");
});

app.listen(3000, () => console.log("Server started"));