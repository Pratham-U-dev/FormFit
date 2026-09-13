import re

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'r') as f:
    content = f.read()

# Remove pullUpMetrics line
content = re.sub(r'\s*val pullUpMetrics by viewModel\.pullUpMetrics\.collectAsState\(\)', '', content)

# Add imports
imports = """
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
"""
content = re.sub(r'import androidx.compose.foundation.layout.\*', 'import androidx.compose.foundation.layout.*\n' + imports, content)

with open('app/src/main/java/com/example/ui/screens/PracticeScreen.kt', 'w') as f:
    f.write(content)
