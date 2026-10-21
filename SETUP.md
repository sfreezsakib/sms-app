# SMS Gateway অ্যাপ — শুধু ফোন দিয়ে APK বানানো

ফোনে Android Studio চলে না, তাই GitHub এর ফ্রি সার্ভারে APK বিল্ড হবে।

## ১. GitHub এ রিপো
1. github.com এ অ্যাকাউন্ট খুলুন।
2. New repository → নাম `sms-app` → Private → Create।
3. Settings → Developer settings → Personal access tokens → Tokens (classic)
   → Generate new → `repo` টিক দিন → টোকেন কপি করে রাখুন।

## ২. ফাইল GitHub এ তোলা (Termux দিয়ে)
sms-app.zip আপনার হোস্টিংয়ের public/ এ রাখুন, তারপর Termux এ:

    pkg install git unzip
    curl -O https://yourdomain.com/sms-app.zip
    unzip sms-app.zip -d sms-app && cd sms-app
    git config --global user.name "you"
    git config --global user.email "you@example.com"
    git init && git add . && git commit -m "init"
    git branch -M main
    git remote add origin https://github.com/USERNAME/sms-app.git
    git push -u origin main

Username চাইলে GitHub username, Password চাইলে ওই টোকেন দিন।
শেষে হোস্টিং থেকে zip মুছে দিন।

(বিকল্প: GitHub এর "Add file → Create new file" এ গিয়ে ফাইলের পাথ লিখে
কোড পেস্ট করুন। পাথ লিখতে `/` দিলেই ফোল্ডার তৈরি হয়।)

## ৩. APK নামানো
1. রিপোতে Actions ট্যাব → "Build APK" → কাজ শেষ হওয়া পর্যন্ত অপেক্ষা (৩-৬ মিনিট)।
2. কাজের পেজের নিচে Artifacts → `SmsGateway-APK` নামান (zip)।
3. zip এক্সট্রাক্ট করে `app-debug.apk` ইনস্টল করুন (Install unknown apps অনুমতি দিন)।
Build ফেইল করলে লগের লাল এরর আমাকে পাঠান।

## ৪. ফোনে অ্যাপ সেটআপ
1. Android 13+ হলে: Settings → Apps → SMS Gateway → ⋮ → **Allow restricted settings**
   (না করলে SMS পারমিশন দেওয়া যাবে না)।
2. অ্যাপ খুলে Server URL ও API Key দিন → Start।
3. SMS ও Notification পারমিশন Allow করুন, ব্যাটারি প্রশ্নে Allow।
4. Battery → Unrestricted, Autostart চালু (Xiaomi/Oppo/Vivo/Samsung),
   Recent apps এ অ্যাপ লক করুন, ফোন চার্জে রাখুন।

সার্ভার API (Laravel) আগের sms-gateway ফাইলগুলোই কাজ করবে, কোনো বদল লাগবে না।
