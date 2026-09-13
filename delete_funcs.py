import re

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'r') as f:
    content = f.read()

# Remove VirtualCoachCanvas
content = re.sub(r'@Composable\s*fun VirtualCoachCanvas\(.*?\)\s*\{.*?(?=\n@Composable|\nfun|\Z)', '', content, flags=re.DOTALL)

# Remove PullUpDashboardHUD
content = re.sub(r'@Composable\s*fun PullUpDashboardHUD\(.*?\)\s*\{.*?(?=\n@Composable|\nfun|\Z)', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'w') as f:
    f.write(content)
