# Android Studio 安装指南（Linux）

## 方法1：使用Snap安装（推荐，最简单）

```bash
# 安装Android Studio
sudo snap install android-studio --classic

# 启动Android Studio
android-studio
```

## 方法2：手动下载安装

### 1. 下载Android Studio

访问官网下载：https://developer.android.com/studio

或使用命令行下载：
```bash
cd ~/Downloads
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.1.1.28/android-studio-2023.1.1.28-linux.tar.gz
```

### 2. 解压到合适位置

```bash
# 解压到/opt目录（需要sudo权限）
sudo tar -xzf android-studio-*-linux.tar.gz -C /opt/

# 或解压到用户目录（不需要sudo）
tar -xzf android-studio-*-linux.tar.gz -C ~/
```

### 3. 启动Android Studio

```bash
# 如果安装在/opt
/opt/android-studio/bin/studio.sh

# 如果安装在用户目录
~/android-studio/bin/studio.sh
```

### 4. 创建桌面快捷方式（可选）

```bash
cat > ~/.local/share/applications/android-studio.desktop << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Icon=/opt/android-studio/bin/studio.png
Exec=/opt/android-studio/bin/studio.sh
Comment=Android Development IDE
Categories=Development;IDE;
Terminal=false
EOF
```

## 首次启动配置

### 1. 欢迎界面

首次启动会看到"Welcome to Android Studio"界面，选择：
- **Do not import settings**（如果是首次安装）

### 2. 安装向导

按照向导步骤：

1. **Install Type**：选择 **Standard**
2. **Select UI Theme**：选择你喜欢的主题（Light/Dark）
3. **Verify Settings**：确认设置
4. **Downloading Components**：等待下载SDK组件（需要网络，可能较慢）

### 3. SDK组件下载

Android Studio会自动下载：
- Android SDK
- Android SDK Platform
- Android SDK Build-Tools
- Android Emulator
- 其他必要组件

**默认SDK安装位置**：`~/Android/Sdk`

## 配置项目使用SDK

### 方法1：创建local.properties（推荐）

在项目根目录创建`local.properties`文件：

```bash
cd /home/tacmon/文档/每日工作记录/my-diary-repo/移动端实现/try_2_apk

# 创建local.properties文件
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

### 方法2：设置环境变量

编辑`~/.bashrc`或`~/.zshrc`：

```bash
# 添加以下行
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

然后重新加载配置：
```bash
source ~/.bashrc  # 或 source ~/.zshrc
```

## 验证安装

### 1. 检查SDK路径

```bash
ls ~/Android/Sdk
# 应该看到：build-tools, platforms, platform-tools等目录
```

### 2. 测试构建

```bash
cd /home/tacmon/文档/每日工作记录/my-diary-repo/移动端实现/try_2_apk
./gradlew assembleDebug
```

如果成功，会在`app/build/outputs/apk/debug/`生成APK文件。

## 常见问题

### 问题1：下载速度慢

**解决方案**：配置国内镜像

编辑`~/.gradle/init.gradle`（如果不存在则创建）：

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://maven.aliyun.com/repository/public' }
    }
}
```

### 问题2：SDK Manager无法下载

**解决方案**：使用命令行工具

```bash
cd ~/Android/Sdk/tools/bin
./sdkmanager --list
./sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 问题3：权限问题

如果遇到权限错误：

```bash
# 修改SDK目录权限
chmod -R 755 ~/Android/Sdk
```

## 最小化安装（仅命令行工具）

如果不需要Android Studio IDE，只需要SDK：

### 1. 下载命令行工具

```bash
cd ~/Downloads
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
```

### 2. 解压并配置

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
unzip commandlinetools-linux-*_latest.zip -d ~/Android/Sdk/cmdline-tools
mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
```

### 3. 安装必要组件

```bash
cd ~/Android/Sdk/cmdline-tools/latest/bin

# 接受许可
./sdkmanager --licenses

# 安装必要组件
./sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 4. 配置环境变量

```bash
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

## 推荐配置

### 1. 增加Gradle内存

编辑`~/.gradle/gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
```

### 2. 配置代理（如果需要）

编辑`~/.gradle/gradle.properties`：

```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
```

## 快速开始

安装完成后，构建APK：

```bash
# 1. 进入项目目录
cd /home/tacmon/文档/每日工作记录/my-diary-repo/移动端实现/try_2_apk

# 2. 创建local.properties（如果还没有）
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 3. 构建Debug版本
./gradlew assembleDebug

# 4. 查看生成的APK
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

## 参考资源

- Android Studio官网：https://developer.android.com/studio
- SDK命令行工具：https://developer.android.com/studio/command-line
- Gradle配置：https://docs.gradle.org/current/userguide/userguide.html
