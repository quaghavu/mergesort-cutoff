@echo off
REM Chay tu bat ky dau: script tu chuyen ve thu muc goc cua project
cd /d "%~dp0.."

echo [1/3] Bien dich Java...
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 (
    echo Loi bien dich. Kiem tra da cai JDK va javac co trong PATH chua.
    exit /b 1
)

echo [2/3] Chay thi nghiem (co the mat vai phut)...
java -Xms2g -Xmx2g -cp out Experiment
if errorlevel 1 (
    echo Thi nghiem that bai.
    exit /b 1
)

echo [3/3] Phan tich ket qua...
python scripts\analyze.py
echo Hoan tat. Xem thu muc results\