# AlienHunt — بسته اصلاح Authentication و Recovery (نسخه آموزشی / DEV)

این بسته **پروژه کامل Android نیست**؛ فقط فایل‌های لازم برای جایگزینی/اضافه کردن را شامل می‌شود.
سورس‌های ارسالی کاربر مبنای اصلاح بوده‌اند. فایل‌های سناریو، نقشه و منطق بازی دست نخورده‌اند.
ZIP عمداً فاقد `backend/.env`، `backend/data/` و `backend/node_modules/` است.

## پیش از جایگزینی
1. از پوشه اصلی `AlienHunt` نسخه پشتیبان بگیرید یا یک Git commit ایجاد کنید.
2. برنامه را ببندید و Backend را با Ctrl+C متوقف کنید.
3. **فایل `.env` فعلی، `data/verification.db` و فایل‌های هم‌تیمی‌ها را حذف نکنید.**
4. محتویات `backend/` این بسته را با پوشه `AlienHunt/backend/` ادغام کنید (فایل‌ها را overwrite کنید؛ پوشه قدیمی را کامل پاک نکنید).
5. محتویات `app/` این بسته را با `AlienHunt/app/` ادغام کنید. فایل‌های متفرقه بازی را دست نزنید.
6. `app/src/main/AndroidManifest.xml` و 3 فایل layout از **نسخه پروژه‌ای که قبلاً ارسال شده بود** ساخته شده‌اند؛ اگر هم‌تیمی‌ها بعداً آن‌ها را تغییر داده‌اند، به جای جایگزینی کورکورانه، تغییرات را ادغام کنید.

## پیکربندی Backend
همان `.env` قبلی باید دارای این مقادیر باشد (مقادیر محرمانه را ارسال نکنید):
```dotenv
NODE_ENV=development
PORT=3000
VERIFICATION_BASE_URL=http://localhost:3000
BREVO_API_KEY=...the-secret-from-your-own-Brevo-account...
BREVO_SENDER_EMAIL=...your-verified-Brevo-sender...
OTP_HMAC_KEY=...64-random-hex-characters...
```
در پروژه واقعی از HTTPS و سرور مرکزی حساب‌ها استفاده کنید. مقدار `OTP_HMAC_KEY` را با کلید تصادفی بسازید.
API Brevo که داخل ZIP خصوصی قبلی قرار داده شده بود، بهتر است در Dashboard سرویس Brevo باطل و کلید جدید جایگزین شود.

## اجرای Backend — PowerShell
داخل پوشه `AlienHunt/backend`:
```powershell
npm.cmd install
node --check server.js
node --check src/routes/verificationRoutes.js
node tests/authService.test.js
node --env-file=.env server.js
```
خروجی آزمون محلی باید با `PASS:` شروع شود. هیچ ایمیل واقعی در این تست ارسال نمی‌شود.
`node --env-file=.env server.js` را باز نگه دارید.

## Android Emulator
در Android Studio پروژه را Sync و Build کنید. `AuthApi.java` آدرس `http://10.0.2.2:3000` را دارد
که **فقط برای Android Emulator روی همان کامپیوتر** مناسب است.
لینک ایمیل `http://localhost:3000/...` را روی **مرورگر همان کامپیوتر ویندوز** باز کنید
نه موبایل. `app/src/debug/AndroidManifest.xml` فقط در Debug به HTTP اجازه می‌دهد.
در دستگاه فیزیکی یا برای انتشار عمومی، سرور HTTPS و Base URL جدا لازم است.

## آزمون دستی نهایی
1. Register: با ایمیل خودتان و رمز حداقل 8 کاراکتری ثبت‌نام کنید.
2. ایمیل Brevo برسد؛ دکمه اول و سپس دکمه دوم Verify Email را روی ویندوز فشار دهید.
3. در صفحه Login همان ایمیل/رمز را وارد کنید. Android با `requestId + clientSecret` وضعیت را چک می‌کند، حساب محلی را فعال می‌کند و وارد `TerminosActivity` می‌شود.
4. Forgot Password: ایمیل تأییدشده را وارد کنید؛ کد 6 رقمی باید به ایمیل برسد، نه در Toast.
5. کد را وارد کنید، رمز جدید را ثبت کنید و دوباره در Login با رمز جدید وارد شوید.
6. کد اشتباه، بیش از 5 تلاش، کد منقضی، و مجوز مصرف‌شده باید رد شوند.
7. بیش از سه درخواست ایمیل در یک ساعت برای همان ایمیل، یا تکرار ظرف 60 ثانیه، باعث پاسخ 429 می‌شود.

## محدودیت‌های معماری فعلی
- در `AndroidManifest.xml` مقدار `android:allowBackup` برای جلوگیری از بکاپ‌گیری خودکار از SQLite حساب‌ها به `false` تغییر کرده است.
- **نسخه آموزشی تک‌دستگاه** است: کاربران و Hash رمز عبور در SQLite گوشی هستند، نه سرور.
  بازیابی روی گوشی دیگر، همگام‌سازی حساب و احراز هویت Multiplayer واقعی **پشتیبانی نمی‌شوند**.
- `requestId` به‌تنهایی مجوز فعال‌سازی نیست؛ بررسی وضعیت به `clientSecret` نیاز دارد.
- رمز جدید به Backend ارسال نمی‌شود. پس از یک مجوز یک‌بارمصرف از سرور، فقط SQLite همان گوشی به‌روزرسانی می‌شود.
- تغییر یا دستکاری نرم‌افزار/SQLite گوشی می‌تواند منطق محلی ورود را دور بزند؛ برای انتشار واقعی، سرور باید مالک حساب، رمز و جلسات احراز هویت باشد.
- رویه تغییر رمز اگر پاسخ سرور موفق شود ولی ذخیره در SQLite شکست بخورد، مجوز مصرف می‌شود؛ در این حالت درخواست OTP تازه لازم است.
- نسخه v2 پایگاه کاربران Android **حساب‌های قدیمی نسخه v1 را پاک می‌کند** (از جمله کاربران پیش‌فرض و رمزهای متن ساده). قبل از نصب نسخه جدید از داده‌های لازم پشتیبان بگیرید.
- فقط صفحه‌های Login/Register/Reset در این بسته ترجمه شده‌اند؛ صفحه‌های سایر اعضای تیم هنوز باید جداگانه بین‌المللی‌سازی شوند.
- زبان فعلاً از locale سیستم/برنامه خوانده می‌شود؛ منوی انتخاب زبان در Settings هنوز پیاده‌سازی نشده است.
- پروژه برای توسعه و آزمایش با `NODE_ENV=development` طراحی شده و API عمومی Production نیست.

## ساختار در یک نگاه
- `backend/src/routes/verificationRoutes.js`: تأیید لینک و API ثبت‌نام، وضعیت، OTP و مصرف مجوز.
- `backend/src/services/otpService.js`: HMAC، 5 دقیقه، 5 تلاش، مجوز 10 دقیقه‌ای یک‌بارمصرف.
- `backend/src/services/emailVerificationService.js`: توکن 24ساعته، clientSecret برای Polling.
- `backend/src/services/rateLimitService.js`: محدودیت ارسال ایمیل.
- `backend/src/templates/renderResetEmail.js`: ایمیل بازیابی چندزبانه.
- `app/src/main/java/com/example/alienhunt/AuthApi.java`: شبکه در پس‌زمینه.
- `app/src/main/java/database/DBHelper.java`: SQLite v2 و حذف seed کاربران.
- `app/src/main/java/database/UserManager.java`: Hash رمز، تأیید حساب و به‌روزرسانی رمز.

**فایل `.env` و اطلاعات حساس را به GitHub Commit نکنید.**
