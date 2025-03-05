const express = require("express")
const cors = require("cors")
const multer = require("multer")
const sharp = require("sharp")
const { createCanvas, loadImage } = require("canvas")
const colorNamer = require("color-namer")

const app = express()
const PORT = process.env.PORT || 5000

// Middleware
app.use(cors())
app.use(express.json({ limit: "50mb" }))
app.use(express.urlencoded({ extended: true, limit: "50mb" }))

// In-memory storage for settings (replace with database in production)
const settings = {
  theme: "dark",
  colorFormat: "hex",
  highPrecisionMode: false,
}

// Helper function to extract dominant colors
const extractColors = async (imageBuffer, numColors = 5, highPrecision = false) => {
  try {
    // Resize image for faster processing
    const resizedImageBuffer = await sharp(imageBuffer)
      .resize({ width: highPrecision ? 300 : 100 })
      .toBuffer()

    // Load image
    const image = await loadImage(resizedImageBuffer)

    // Create canvas and draw image
    const canvas = createCanvas(image.width, image.height)
    const ctx = canvas.getContext("2d")
    ctx.drawImage(image, 0, 0)

    // Get image data
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
    const pixels = imageData.data

    // Simple color quantization (for demo purposes)
    const colorMap = {}
    const step = highPrecision ? 1 : 4 // Sample every 4 pixels in low precision mode

    for (let i = 0; i < pixels.length; i += 4 * step) {
      const r = pixels[i]
      const g = pixels[i + 1]
      const b = pixels[i + 2]

      // Skip transparent pixels
      if (pixels[i + 3] < 128) continue

      // Quantize colors slightly to group similar colors
      const quantLevel = highPrecision ? 8 : 24
      const qR = Math.round(r / quantLevel) * quantLevel
      const qG = Math.round(g / quantLevel) * quantLevel
      const qB = Math.round(b / quantLevel) * quantLevel

      const colorKey = `${qR},${qG},${qB}`

      if (!colorMap[colorKey]) {
        colorMap[colorKey] = {
          r: qR,
          g: qG,
          b: qB,
          count: 0,
        }
      }

      colorMap[colorKey].count++
    }

    // Sort colors by frequency
    const sortedColors = Object.values(colorMap)
      .sort((a, b) => b.count - a.count)
      .slice(0, numColors)

    // Convert to hex and get color names
    return sortedColors.map((color) => {
      const { r, g, b } = color
      const hex = rgbToHex(r, g, b)
      const colorNames = colorNamer(hex)

      return {
        hex,
        name: colorNames.ntc[0].name, // Use the NTC color names
        rgb: { r, g, b },
      }
    })
  } catch (error) {
    console.error("Error extracting colors:", error)
    throw error
  }
}

// Helper function to convert RGB to HEX
const rgbToHex = (r, g, b) => {
  return (
    "#" +
    [r, g, b]
      .map((x) => {
        const hex = x.toString(16)
        return hex.length === 1 ? "0" + hex : hex
      })
      .join("")
  )
}

// Routes
app.post("/api/colors/analyze", async (req, res) => {
  try {
    const { imageData } = req.body

    if (!imageData) {
      return res.status(400).json({ message: "Image data is required" })
    }

    // Convert base64 to buffer
    const base64Data = imageData.replace(/^data:image\/\w+;base64,/, "")
    const imageBuffer = Buffer.from(base64Data, "base64")

    // Extract colors
    const colors = await extractColors(imageBuffer, 5, settings.highPrecisionMode)

    // In a real app, you would upload the image to cloud storage
    // For demo, we'll just return the image data URL
    return res.status(200).json({
      imageUrl: imageData,
      colors,
    })
  } catch (error) {
    console.error("Error analyzing image:", error)
    return res.status(500).json({ message: "Error analyzing image" })
  }
})

app.get("/api/settings", (req, res) => {
  res.json(settings)
})

app.put("/api/settings", (req, res) => {
  const { settings: newSettings } = req.body

  if (newSettings) {
    // Update settings
    if (newSettings.theme) settings.theme = newSettings.theme
    if (newSettings.colorFormat) settings.colorFormat = newSettings.colorFormat
    if (newSettings.highPrecisionMode !== undefined) {
      settings.highPrecisionMode = newSettings.highPrecisionMode
    }
  }

  res.json(settings)
})

// Start server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`)
})

