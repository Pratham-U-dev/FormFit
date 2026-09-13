"""
Pull-Up Biomechanics and Real-Time Form Analysis Module
Based on inverse dynamics, 3-state motor unit fatigue (Xia & Frey-Law),
Hill force-velocity mechanics, and thermal muscle models.
"""

from .biomechanics import (
    PullUpBiomechanicsAnalyzer,
    RepRecord,
    MuscleState,
    BlazePoseLandmarks,
)
from .heatmap import MuscleHeatmapRenderer, ColorGradient
from .dashboard_renderer import PullUpDashboardRenderer
from .pullup_module import PullUpExerciseModule

__all__ = [
    "PullUpBiomechanicsAnalyzer",
    "RepRecord",
    "MuscleState",
    "BlazePoseLandmarks",
    "MuscleHeatmapRenderer",
    "ColorGradient",
    "PullUpDashboardRenderer",
    "PullUpExerciseModule",
]
