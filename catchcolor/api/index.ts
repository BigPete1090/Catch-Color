import axios from "axios"

const API_URL = "https://your-api-url.com/api" // Replace with your actual API URL

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
})

export const analyzeImage = async (imageData: string) => {
  try {
    const response = await api.post("/colors/analyze", { imageData })
    return response.data
  } catch (error) {
    console.error("Error analyzing image:", error)
    throw error
  }
}

export const updateUserSettings = async (settings: any) => {
  try {
    const response = await api.put("/settings", { settings })
    return response.data
  } catch (error) {
    console.error("Error updating settings:", error)
    throw error
  }
}

export const getUserSettings = async () => {
  try {
    const response = await api.get("/settings")
    return response.data
  } catch (error) {
    console.error("Error fetching settings:", error)
    // Return default settings if API fails
    return {
      theme: "dark",
      colorFormat: "hex",
      highPrecisionMode: false,
    }
  }
}

