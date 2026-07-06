@rem
@rem Gradle startup script for Windows
@rem
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set GRADLE_HOME=%APP_HOME%gradle\wrapper
set JAR_PATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

@rem Set JAVA_HOME
if "%JAVA_HOME%"=="" (
    set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
)

@rem Execute Gradle
"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "%JAR_PATH%" org.gradle.wrapper.GradleWrapperMain %*
