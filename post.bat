@echo off
setlocal EnableExtensions

cd /d "%~dp0"

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo This folder is not a git repository.
    exit /b 1
)

set "REMOTE=origin"
set "BRANCH="
for /f "delims=" %%I in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "BRANCH=%%I"
if not defined BRANCH (
    echo Could not determine the current branch.
    exit /b 1
)

git remote get-url "%REMOTE%" >nul 2>&1
if errorlevel 1 (
    echo Remote "%REMOTE%" is not configured.
    exit /b 1
)

set "DRY_RUN="
set "MESSAGE="
if /i "%~1"=="--dry-run" (
    set "DRY_RUN=1"
    set "MESSAGE=%~2"
) else (
    set "MESSAGE=%~1"
)

if not defined MESSAGE (
    set "MESSAGE=Update %DATE% %TIME%"
)

echo Repository: %CD%
echo Remote: %REMOTE%
echo Branch: %BRANCH%
echo Commit message: %MESSAGE%
echo.

git status --short

if defined DRY_RUN (
    echo.
    git add -A --dry-run
    echo.
    echo Dry run only. Nothing was committed or pushed.
    exit /b 0
)

git add -A
git diff --cached --quiet
if not errorlevel 1 (
    echo.
    echo No staged changes to post.
    exit /b 0
)

git commit -m "%MESSAGE%"
if errorlevel 1 (
    echo.
    echo Commit failed.
    exit /b 1
)

git push "%REMOTE%" "%BRANCH%"
if errorlevel 1 (
    echo.
    echo Push failed.
    exit /b 1
)

echo.
echo Posted current code to GitHub.
exit /b 0
