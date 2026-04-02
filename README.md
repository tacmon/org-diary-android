# Org日记移动端APK

## 概述
原生Android应用，用于在手机上快速添加条目到main.org日记文件。

## 功能特性
- ✅ 快速添加带时间戳的条目
- ✅ 支持自定义时间或使用当前时间
- ✅ 直接插入到main.org文件
- ✅ 与GitHub仓库同步
- ✅ 简洁的移动端UI

## 技术栈
- Kotlin
- Jetpack Compose (现代UI)
- JGit (Git操作)
- Coroutines (异步处理)

## 构建说明
```bash
cd 移动端实现/try_2_apk
./gradlew assembleDebug
```

生成的APK位于：`app/build/outputs/apk/debug/app-debug.apk`

## 安装
将APK传输到手机并安装（需要允许安装未知来源应用）

## 使用方法
1. 首次打开配置GitHub仓库信息
2. 点击"添加条目"按钮
3. 选择时间（或使用当前时间）
4. 输入条目内容
5. 点击"插入"保存并同步
