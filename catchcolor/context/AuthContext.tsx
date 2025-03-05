"use client"

import type React from "react"
import { createContext, useContext, useState, useEffect } from "react"
import { type AuthState, getCurrentUser, signIn, signOut } from "../services/auth"

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const initialState: AuthState = {
  user: null,
  token: null,
  isLoading: true,
  error: null,
}

const AuthContext = createContext<AuthContextType>({
  ...initialState,
  login: async () => {},
  logout: async () => {},
})

export const useAuth = () => useContext(AuthContext)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AuthState>(initialState)

  useEffect(() => {
    // Check if user is already logged in
    const loadUser = async () => {
      try {
        const user = await getCurrentUser()
        setState({
          user,
          token: user ? "token-exists" : null, // Simplified for demo
          isLoading: false,
          error: null,
        })
      } catch (error) {
        setState({
          user: null,
          token: null,
          isLoading: false,
          error: "Failed to load user",
        })
      }
    }

    loadUser()
  }, [])

  const login = async (email: string, password: string) => {
    try {
      setState({ ...state, isLoading: true, error: null })
      const user = await signIn(email, password)
      setState({
        user,
        token: "token-exists", // Simplified for demo
        isLoading: false,
        error: null,
      })
    } catch (error) {
      setState({
        ...state,
        isLoading: false,
        error: "Authentication failed",
      })
    }
  }

  const logout = async () => {
    try {
      setState({ ...state, isLoading: true, error: null })
      await signOut()
      setState({
        user: null,
        token: null,
        isLoading: false,
        error: null,
      })
    } catch (error) {
      setState({
        ...state,
        isLoading: false,
        error: "Logout failed",
      })
    }
  }

  return <AuthContext.Provider value={{ ...state, login, logout }}>{children}</AuthContext.Provider>
}

