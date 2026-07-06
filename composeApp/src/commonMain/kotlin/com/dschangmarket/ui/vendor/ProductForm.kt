package com.dschangmarket.ui.vendor

data class ProductForm(
    val productId: Int = 0,
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val comparePrice: String = "",
    val category: String = "",
    val stock: String = "",
    val unit: String = "pièce",
    val imageUrls: List<String> = emptyList(), // Existing URLs
    val newImages: List<NewImageData> = emptyList(), // Local images to upload
    val isStory: Boolean = false
)

data class NewImageData(
    val dataUrl: String,
    val fileName: String
)
