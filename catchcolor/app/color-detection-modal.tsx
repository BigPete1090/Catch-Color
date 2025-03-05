"use client"

import type React from "react"

import { useState, useEffect } from "react"
import Image from "next/image"
import { ArrowLeft, Share2, ZoomIn, ZoomOut } from "lucide-react"
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { analyzeImage } from "@/services/imageService"
import { Spinner } from "@/components/ui/spinner"

interface ColorDetectionModalProps {
  isOpen: boolean
  onClose: () => void
  imageSrc: string | null
}

export default function ColorDetectionModal({ isOpen, onClose, imageSrc }: ColorDetectionModalProps) {
  const [scale, setScale] = useState(1)
  const [selectedColor, setSelectedColor] = useState<{
    name: string
    hex: string
    rgb?: { r: number; g: number; b: number }
  } | null>(null)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [colors, setColors] = useState<
    Array<{
      name: string
      hex: string
      rgb: { r: number; g: number; b: number }
    }>
  >([])

  useEffect(() => {
    if (isOpen && imageSrc) {
      analyzeImageColors(imageSrc)
    } else {
      // Reset state when modal closes
      setSelectedColor(null)
      setColors([])
      setScale(1)
    }
  }, [isOpen, imageSrc])

  const analyzeImageColors = async (imageUri: string) => {
    try {
      setIsAnalyzing(true)
      const result = await analyzeImage(imageUri)

      if (result) {
        setColors(result.colors)
        // Select the first color by default
        if (result.colors.length > 0) {
          setSelectedColor(result.colors[0])
        }
      }
    } catch (error) {
      console.error("Error analyzing colors:", error)
    } finally {
      setIsAnalyzing(false)
    }
  }

  const handleZoomIn = () => {
    setScale((prev) => Math.min(prev + 0.25, 3))
  }

  const handleZoomOut = () => {
    setScale((prev) => Math.max(prev - 0.25, 0.5))
  }

  const handleImageClick = (e: React.MouseEvent<HTMLDivElement>) => {
    // In a real implementation, we would detect the color at the clicked position
    // For now, we'll just cycle through the available colors
    if (colors.length > 0) {
      const currentIndex = selectedColor ? colors.findIndex((c) => c.hex === selectedColor.hex) : -1
      const nextIndex = (currentIndex + 1) % colors.length
      setSelectedColor(colors[nextIndex])
    }
  }

  const handleShare = async () => {
    // Implement share functionality
    // This would typically use the Share API on mobile
    console.log("Sharing color:", selectedColor)
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-full sm:max-w-[600px] h-[80vh] flex flex-col p-0 bg-black/95 border-white/20">
        <DialogHeader className="p-4 border-b border-white/10">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={onClose} className="rounded-full hover:bg-white/10">
              <ArrowLeft className="h-5 w-5" />
              <span className="sr-only">Back</span>
            </Button>
            <h2 className="text-white font-medium">Analyze Colors</h2>
          </div>
        </DialogHeader>

        {/* Image Container */}
        <div className="relative flex-1 overflow-hidden bg-black/50">
          {isAnalyzing ? (
            <div className="absolute inset-0 flex items-center justify-center">
              <Spinner size="lg" />
              <span className="ml-2 text-white">Analyzing colors...</span>
            </div>
          ) : (
            imageSrc && (
              <div
                className="w-full h-full cursor-crosshair"
                onClick={handleImageClick}
                style={{
                  transform: `scale(${scale})`,
                  transition: "transform 0.2s ease-out",
                }}
              >
                <Image src={imageSrc || "/placeholder.svg"} alt="Selected image" fill className="object-contain" />
              </div>
            )
          )}

          {/* Zoom Controls */}
          <div className="absolute bottom-4 right-4 flex gap-2">
            <Button
              variant="secondary"
              size="icon"
              onClick={handleZoomOut}
              disabled={scale <= 0.5 || isAnalyzing}
              className="bg-white/10 hover:bg-white/20"
            >
              <ZoomOut className="h-4 w-4" />
              <span className="sr-only">Zoom Out</span>
            </Button>
            <Button
              variant="secondary"
              size="icon"
              onClick={handleZoomIn}
              disabled={scale >= 3 || isAnalyzing}
              className="bg-white/10 hover:bg-white/20"
            >
              <ZoomIn className="h-4 w-4" />
              <span className="sr-only">Zoom In</span>
            </Button>
          </div>

          {/* Color Info Overlay */}
          {selectedColor && !isAnalyzing && (
            <div className="absolute bottom-4 left-4 bg-black/80 backdrop-blur-md p-4 rounded-lg border border-white/20">
              <div className="flex items-center gap-3">
                <div
                  className="w-8 h-8 rounded-full border border-white/20"
                  style={{ backgroundColor: selectedColor.hex }}
                />
                <div>
                  <p className="font-medium text-white">{selectedColor.name}</p>
                  <p className="text-sm text-gray-400">{selectedColor.hex}</p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Actions Footer */}
        <div className="p-4 border-t border-white/10 flex justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            className="border-white/20 bg-white/5 hover:bg-white/10"
            onClick={handleShare}
            disabled={isAnalyzing || !selectedColor}
          >
            <Share2 className="h-4 w-4 mr-2" />
            Share
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}

