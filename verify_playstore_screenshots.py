import os
import sys
from PIL import Image, ImageStat

sys.stdout.reconfigure(encoding='utf-8')

PLAYSTORE_ASSETS_DIR = r"C:\Users\cucos\AndroidStudioProjects\Shrinkpdf\PlayStore_Assets"

LOCALES = [
    "en-US", "de-DE", "es-ES", "fr-FR", 
    "id-ID", "it-IT", "pt-PT", "pt-BR", "ro-RO"
]

if len(sys.argv) > 1 and sys.argv[1] in LOCALES:
    LOCALES = [sys.argv[1]]

if len(sys.argv) > 1 and sys.argv[1] in LOCALES:
    LOCALES = [sys.argv[1]]

CATEGORIES = {
    "1_Phone_Screenshots": {"expected_count": 4, "allowed_resolutions": [(1080, 2400), (1080, 1920), (1440, 3120)]},
    "5_Tablet_10inch_Screenshots": {"expected_count": 4, "allowed_resolutions": [(1600, 2560), (2560, 1600), (1200, 1920)]},
    "4_Tablet_7inch_Screenshots": {"expected_count": 4, "allowed_resolutions": [(1200, 1920)]}
}

EXPECTED_FILES = ["1_Home.png", "2_Compress.png", "3_Organize.png", "4_Create.png"]

def is_image_blank_or_corrupt(filepath):
    try:
        with Image.open(filepath) as img:
            img.verify()
        with Image.open(filepath) as img:
            stat = ImageStat.Stat(img.convert("L"))
            # Standard deviation check for blank/solid color screen
            if stat.stddev[0] < 5.0:
                return True, "Image is blank or solid color"
            return False, f"Valid ({img.width}x{img.height})"
    except Exception as e:
        return True, f"Corrupt image file: {e}"

def verify_screenshots():
    print("=========================================================")
    print("      PLAY STORE SCREENSHOT VERIFICATION SUITE")
    print("=========================================================\n")
    
    total_checks = 0
    passed_checks = 0
    failed_details = []

    for locale in LOCALES:
        print(f"--- Checking Locale: {locale} ---")
        for cat_name, rules in CATEGORIES.items():
            cat_dir = os.path.join(PLAYSTORE_ASSETS_DIR, locale, cat_name)
            total_checks += 1
            
            if not os.path.exists(cat_dir):
                print(f"  ❌ [{cat_name}] Directory MISSING: {cat_dir}")
                failed_details.append((locale, cat_name, "Directory missing"))
                continue

            missing_files = []
            resolution_issues = []
            corrupt_issues = []

            for filename in EXPECTED_FILES:
                file_path = os.path.join(cat_dir, filename)
                if not os.path.exists(file_path):
                    missing_files.append(filename)
                else:
                    is_bad, msg = is_image_blank_or_corrupt(file_path)
                    if is_bad:
                        corrupt_issues.append(f"{filename}: {msg}")
                    else:
                        with Image.open(file_path) as img:
                            dim = (img.width, img.height)
                            if dim not in rules["allowed_resolutions"]:
                                resolution_issues.append(f"{filename} ({dim[0]}x{dim[1]})")

            duplicate_issues = []
            # Check for duplicate identical screens
            valid_paths = [os.path.join(cat_dir, f) for f in EXPECTED_FILES if os.path.exists(os.path.join(cat_dir, f))]
            for i in range(len(valid_paths)):
                for j in range(i + 1, len(valid_paths)):
                    try:
                        from PIL import ImageChops
                        with Image.open(valid_paths[i]) as im1, Image.open(valid_paths[j]) as im2:
                            diff = ImageChops.difference(im1.convert("RGB"), im2.convert("RGB"))
                            if not diff.getbbox():
                                duplicate_issues.append(f"{os.path.basename(valid_paths[i])} == {os.path.basename(valid_paths[j])}")
                    except Exception:
                        pass

            if missing_files or resolution_issues or corrupt_issues or duplicate_issues:
                print(f"  ❌ [{cat_name}] FAIL")
                if missing_files:
                    print(f"      Missing: {', '.join(missing_files)}")
                if resolution_issues:
                    print(f"      Unexpected dimensions: {', '.join(resolution_issues)}")
                if corrupt_issues:
                    print(f"      Integrity issues: {', '.join(corrupt_issues)}")
                if duplicate_issues:
                    print(f"      Duplicate screens: {', '.join(duplicate_issues)}")
                failed_details.append((locale, cat_name, "Issues found"))
            else:
                passed_checks += 1
                print(f"  ✅ [{cat_name}] PASS (4/4 files verified)")

        print()

    print("=========================================================")
    print(f" SUMMARY: {passed_checks}/{total_checks} Test Suites Passed")
    print("=========================================================")
    
    if failed_details:
        print("\nFailed Categories Summary:")
        for loc, cat, reason in failed_details:
            print(f" - {loc} -> {cat}: {reason}")
        return False
    else:
        print("\n🎉 ALL SCREENSHOTS VERIFIED PERFECTLY!")
        return True

if __name__ == "__main__":
    success = verify_screenshots()
    sys.exit(0 if success else 1)
