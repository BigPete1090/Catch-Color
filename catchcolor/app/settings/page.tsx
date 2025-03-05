"use client"

import { useState, useEffect } from "react"
import { ArrowLeft, Moon, Eye, Palette } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import Link from "next/link"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { getUserSettings, updateUserSettings } from "@/api"
import { Spinner } from "@/components/ui/spinner"

export default function SettingsPage() {
  const [theme, setTheme] = useState<"dark" | "light">("dark")
  const [colorFormat, setColorFormat] = useState<"hex" | "rgb" | "hsl">("hex")
  const [highPrecisionMode, setHighPrecisionMode] = useState(false)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    loadSettings()
  }, [])

  const loadSettings = async () => {
    try {
      setIsLoading(true)
      const settings = await getUserSettings()

      if (settings) {
        setTheme(settings.theme || "dark")
        setColorFormat(settings.colorFormat || "hex")
        setHighPrecisionMode(settings.highPrecisionMode || false)
      }
    } catch (error) {
      console.error("Error loading settings:", error)
      // Use defaults if API fails
      setTheme("dark")
      setColorFormat("hex")
      setHighPrecisionMode(false)
    } finally {
      setIsLoading(false)
    }
  }

  const handleThemeChange = async (value: "dark" | "light") => {
    setTheme(value)
    try {
      await updateUserSettings({
        theme: value,
        colorFormat,
        highPrecisionMode,
      })
    } catch (error) {
      console.error("Error saving theme setting:", error)
    }
  }

  const handleColorFormatChange = async (value: "hex" | "rgb" | "hsl") => {
    setColorFormat(value)
    try {
      await updateUserSettings({
        theme,
        colorFormat: value,
        highPrecisionMode,
      })
    } catch (error) {
      console.error("Error saving color format setting:", error)
    }
  }

  const handlePrecisionModeChange = async (checked: boolean) => {
    setHighPrecisionMode(checked)
    try {
      await updateUserSettings({
        theme,
        colorFormat,
        highPrecisionMode: checked,
      })
    } catch (error) {
      console.error("Error saving precision mode setting:", error)
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen w-full bg-black text-white flex items-center justify-center">
        <Spinner size="lg" />
        <span className="ml-2">Loading settings...</span>
      </div>
    )
  }

  return (
    <div className="min-h-screen w-full bg-black text-white">
      <div className="container max-w-md mx-auto px-4 py-8">
        {/* Header */}
        <header className="flex items-center gap-4 mb-8">
          <Link href="/">
            <Button variant="ghost" size="icon" className="rounded-full hover:bg-white/10">
              <ArrowLeft className="h-6 w-6" />
              <span className="sr-only">Back</span>
            </Button>
          </Link>
          <h1 className="text-2xl font-bold">Settings</h1>
        </header>

        {/* Settings Groups */}
        <div className="space-y-8">
          {/* Appearance */}
          <section className="space-y-4">
            <h2 className="text-lg font-semibold text-gray-400">Appearance</h2>
            <div className="space-y-4 rounded-2xl bg-white/5 p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="rounded-full bg-white/10 p-2">
                    <Moon className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="font-medium">Theme</p>
                    <p className="text-sm text-gray-400">Adjust app appearance</p>
                  </div>
                </div>
                <Select value={theme} onValueChange={handleThemeChange}>
                  <SelectTrigger className="w-32 bg-transparent border-white/20">
                    <SelectValue placeholder="Select theme" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="dark">Dark</SelectItem>
                    <SelectItem value="light">Light</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </section>

          {/* Color Detection */}
          <section className="space-y-4">
            <h2 className="text-lg font-semibold text-gray-400">Color Detection</h2>
            <div className="space-y-4 rounded-2xl bg-white/5 p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="rounded-full bg-white/10 p-2">
                    <Palette className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="font-medium">High Precision Mode</p>
                    <p className="text-sm text-gray-400">More accurate but slower</p>
                  </div>
                </div>
                <Switch checked={highPrecisionMode} onCheckedChange={handlePrecisionModeChange} />
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="rounded-full bg-white/10 p-2">
                    <Eye className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="font-medium">Color Format</p>
                    <p className="text-sm text-gray-400">Choose display format</p>
                  </div>
                </div>
                <Select value={colorFormat} onValueChange={handleColorFormatChange}>
                  <SelectTrigger className="w-32 bg-transparent border-white/20">
                    <SelectValue placeholder="Select format" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="hex">HEX</SelectItem>
                    <SelectItem value="rgb">RGB</SelectItem>
                    <SelectItem value="hsl">HSL</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

