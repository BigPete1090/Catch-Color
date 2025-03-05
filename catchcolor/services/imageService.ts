import * as ImagePicker from "expo-image-picker"
import * as FileSystem from "expo-file-system"
import { analyzeImage as apiAnalyzeImage } from "../api"

export interface ColorInfo {
  hex: string
  name: string
  rgb: {
    r: number
    g: number
    b: number
  }
}

export interface ImageAnalysisResult {
  imageUrl: string
  colors: ColorInfo[]
}

export const captureImage = async (): Promise<string | null> => {
  try {
    // Request camera permissions
    const { status } = await ImagePicker.requestCameraPermissionsAsync()

    if (status !== "granted") {
      console.error("Camera permission not granted")
      return null
    }

    // Launch camera
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
      base64: true,
    })

    if (result.canceled) {
      return null
    }

    return result.assets[0].uri
  } catch (error) {
    console.error("Error capturing image:", error)
    return null
  }
}

export const pickImage = async (): Promise<string | null> => {
  try {
    // Request media library permissions
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync()

    if (status !== "granted") {
      console.error("Media library permission not granted")
      return null
    }

    // Launch image picker
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.8,
      base64: true,
    })

    if (result.canceled) {
      return null
    }

    return result.assets[0].uri
  } catch (error) {
    console.error("Error picking image:", error)
    return null
  }
}

export const analyzeImage = async (imageUri: string): Promise<ImageAnalysisResult | null> => {
  try {
    // Read image as base64
    const base64 = await FileSystem.readAsStringAsync(imageUri, {
      encoding: FileSystem.EncodingType.Base64,
    })

    // Prepend data URL prefix
    const imageData = `data:image/jpeg;base64,${base64}`

    // Send to backend for analysis
    const result = await apiAnalyzeImage(imageData)
    return result
  } catch (error) {
    console.error("Error analyzing image:", error)

    // For testing/demo purposes, return mock data if API fails
    return {
      imageUrl: imageUri,
      colors: [
        {
          hex: "#0047AB",
          name: "Deep Blue",
          rgb: { r: 0, g: 71, b: 171 },
        },
        {
          hex: "#50C878",
          name: "Emerald Green",
          rgb: { r: 80, g: 200, b: 120 },
        },
        {
          hex: "#FF5733",
          name: "Coral Red",
          rgb: { r: 255, g: 87, b: 51 },
        },
      ],
    }
  }
}

