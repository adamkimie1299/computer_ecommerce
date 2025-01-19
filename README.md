## Project Setup Instructions

### Image Handling
1. Create a directory `src/main/webapp/images` if it doesn't exist
2. Ensure the `images` directory has write permissions
3. Add a placeholder image named `placeholder.jpg` in the `images` directory
4. When deploying the application, ensure the `images` directory is included in the deployment

### Database Setup
1. Ensure the image URLs in the `products` table are relative paths (e.g., `images/product1.jpg`)
2. Update any absolute paths in the database to use relative paths