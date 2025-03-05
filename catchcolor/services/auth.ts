import AsyncStorage from "@react-native-async-storage/async-storage"
import axios from "axios"

const API_URL = "https://your-api-url.com/api" // Replace with your actual API URL

export interface User {
  uid: string
  email: string
  displayName?: string
}

export interface AuthState {
  user: User | null
  token: string | null
  isLoading: boolean
  error: string | null
}

// Mock implementation - replace with actual Firebase or Auth0 implementation
export const signIn = async (email: string, password: string): Promise<User> => {
  try {
    const response = await axios.post(`${API_URL}/auth/login`, {
      email,
      password,
    })

    const { user, token } = response.data

    // Store auth token
    await AsyncStorage.setItem("authToken", token)
    await AsyncStorage.setItem("user", JSON.stringify(user))

    return user
  } catch (error) {
    console.error("Sign in error:", error)
    throw new Error("Authentication failed")
  }
}

export const signOut = async (): Promise<void> => {
  try {
    await AsyncStorage.removeItem("authToken")
    await AsyncStorage.removeItem("user")
  } catch (error) {
    console.error("Sign out error:", error)
    throw new Error("Sign out failed")
  }
}

export const getCurrentUser = async (): Promise<User | null> => {
  try {
    const userJson = await AsyncStorage.getItem("user")
    if (!userJson) return null
    return JSON.parse(userJson)
  } catch (error) {
    console.error("Get current user error:", error)
    return null
  }
}

export const isAuthenticated = async (): Promise<boolean> => {
  const token = await AsyncStorage.getItem("authToken")
  return !!token
}

