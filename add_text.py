import sys
from PIL import Image, ImageDraw, ImageFont

def main():
    try:
        # Open the image
        img = Image.open("app_icon_fengshui.png").convert("RGBA")
        draw = ImageDraw.Draw(img)
        
        text = "PDFchemy\nTools"
        
        # Try to use a nice font, fallback to default
        try:
            # Segoe UI Bold on Windows
            font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 110)
        except Exception:
            font = ImageFont.load_default()
            print("Failed to load Segoe UI, using default font.")
            
        # Text size and position
        # Get bounding box for text
        bbox = draw.multiline_textbbox((0, 0), text, font=font, align="center")
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
        
        # Position at bottom center
        width, height = img.size
        x = (width - text_width) / 2
        y = height - text_height - 60
        
        # Draw soft drop shadow for readability
        shadow_color = (0, 0, 0, 180)
        draw.multiline_text((x+4, y+4), text, font=font, fill=shadow_color, align="center")
        draw.multiline_text((x+2, y+2), text, font=font, fill=shadow_color, align="center")
        
        # Draw text in white
        draw.multiline_text((x, y), text, font=font, fill=(255, 255, 255, 255), align="center")
        
        # Save output
        img.save("app_icon_launcher.png")
        print("Successfully generated app_icon_launcher.png")
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
