"use client"

import { useState } from "react"
import { Camera, Settings2, ImageIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import ColorDetectionModal from "./color-detection-modal"
import Link from "next/link"
import { captureImage, pickImage } from "@/services/imageService"

export default function HomePage() {
  const [showModal, setShowModal] = useState(false)
  const [selectedImage, setSelectedImage] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleCapture = async () => {
    try {
      setIsLoading(true)
      const imageUri = await captureImage()

      if (imageUri) {
        setSelectedImage(imageUri)
        setShowModal(true)
      }
    } catch (error) {
      console.error("Error capturing image:", error)
    } finally {
      setIsLoading(false)
    }
  }

  const handleUpload = async () => {
    try {
      setIsLoading(true)
      const imageUri = await pickImage()

      if (imageUri) {
        setSelectedImage(imageUri)
        setShowModal(true)
      }
    } catch (error) {
      console.error("Error uploading image:", error)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen w-full bg-black text-white">
      <div className="container max-w-md mx-auto px-4 py-8">
        {/* Header */}
        <header className="flex justify-between items-center mb-12">
          <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
            Catch Color
          </h1>
          <Link href="/settings">
            <Button variant="ghost" size="icon" className="rounded-full hover:bg-white/10">
              <Settings2 className="h-6 w-6" />
              <span className="sr-only">Settings</span>
            </Button>
          </Link>
        </header>

        {/* Main Content */}
        <main className="space-y-8">
          <div className="grid gap-6 text-center">
            <Button
              size="lg"
              className="h-40 w-40 mx-auto rounded-full bg-gradient-to-br from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 shadow-lg hover:shadow-blue-500/25 transition-all duration-300"
              onClick={handleCapture}
              disabled={isLoading}
            >
              <Camera className="h-12 w-12" />
              <span className="sr-only">Capture Photo</span>
            </Button>
            <p className="text-lg font-medium text-gray-300">Tap to Capture</p>
          </div>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-white/10" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-black px-2 text-gray-500">or</span>
            </div>
          </div>

          <Button
            variant="outline"
            size="lg"
            className="w-full h-16 text-lg border-white/20 bg-white/5 hover:bg-white/10 backdrop-blur-sm transition-all duration-300"
            onClick={handleUpload}
            disabled={isLoading}
          >
            <ImageIcon className="mr-2 h-5 w-5" />
            Upload Image
          </Button>
        </main>

        {/* Color Detection Modal */}
        <ColorDetectionModal isOpen={showModal} onClose={() => setShowModal(false)} imageSrc={selectedImage} />
      </div>
    </div>
  )
}

