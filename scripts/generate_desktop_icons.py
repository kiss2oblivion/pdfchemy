import sys
import os
from PIL import Image

sys.stdout.reconfigure(encoding='utf-8')

def generate_icons():
    src_icon = r"E:\backup_ext_hdd\android-projects-2026-07-30\Shrinkpdf\PlayStore_Assets\PLAYSTORE_AppIcon_512x512.png"
    if not os.path.exists(src_icon):
        print(f"Source icon not found: {src_icon}")
        return

    img = Image.open(src_icon).convert("RGBA")

    # Target directories
    win_dir = r"E:\backup_ext_hdd\android-projects-2026-07-30\Shrinkpdf\desktop\src\main\resources\icons\windows"
    linux_dir = r"E:\backup_ext_hdd\android-projects-2026-07-30\Shrinkpdf\desktop\src\main\resources\icons\linux"

    os.makedirs(win_dir, exist_ok=True)
    os.makedirs(linux_dir, exist_ok=True)

    # 1. Windows ICO
    ico_sizes = [(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    ico_path = os.path.join(win_dir, "icon.ico")
    img.save(ico_path, format="ICO", sizes=ico_sizes)
    print(f"Generated Windows multi-resolution icon: {ico_path}")

    # 2. Linux PNG 512x512
    linux_png_path = os.path.join(linux_dir, "icon.png")
    img.save(linux_png_path, format="PNG")
    print(f"Generated Linux master icon: {linux_png_path}")

    # 3. Linux Hicolor Icon Theme directories
    hicolor_dir = os.path.join(linux_dir, "hicolor")
    sizes = [16, 32, 48, 64, 128, 256, 512]
    for s in sizes:
        sub_dir = os.path.join(hicolor_dir, f"{s}x{s}", "apps")
        os.makedirs(sub_dir, exist_ok=True)
        resized = img.resize((s, s), Image.Resampling.LANCZOS)
        out_file = os.path.join(sub_dir, "pdfchemy.png")
        resized.save(out_file, format="PNG")
        print(f"Generated Linux hicolor {s}x{s} icon: {out_file}")

if __name__ == "__main__":
    generate_icons()
