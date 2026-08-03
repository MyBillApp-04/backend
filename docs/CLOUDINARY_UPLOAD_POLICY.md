# Cloudinary upload policy

Production must use the `mybill_restricted_images` signed upload preset (or the name set in `CLOUDINARY_UPLOAD_PRESET`). Configure the preset to reject unsigned uploads and enforce these server-matched restrictions:

- resource type: `image`
- formats: PNG, JPG/JPEG, or WebP
- maximum asset size: 5 MiB
- maximum dimensions: 4096 × 4096 pixels
- no client-selected folder or public ID; uploads must use the values returned by the signature endpoint

The backend issues signatures for `mybill/{userId}/{logo|qr|signature}` only. A metadata submission is accepted only when its Cloudinary response signature validates against `CLOUDINARY_API_SECRET`, its public ID is in that user's expected folder/type, and its URL, resource type, format, bytes, and dimensions satisfy the policy. Keep `CLOUDINARY_API_SECRET` server-only.
