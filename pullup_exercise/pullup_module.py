"""
pullup_exercise.pullup_module
=============================
Unified high-level entry-point for real-time Pull-Up analysis, thermal heat map
generation, and dashboard UI rendering.

Example usage:
--------------
    from pullup_exercise import PullUpExerciseModule

    # Initialize module with athlete biometric parameters
    analyzer = PullUpExerciseModule(height_m=1.88, mass_kg=79.0, fps=30.0)

    # In your MediaPipe / BlazePose camera loop:
    for frame, pose_landmarks in camera_stream:
        # Update metrics with raw landmarks
        metrics = analyzer.update(pose_landmarks)

        # Render complete HUD + muscle thermal overlay onto frame
        annotated_frame = analyzer.render(frame, pose_landmarks)

    # End of set
    summary = analyzer.finish_set()
    print("Set completed:", summary["total_reps"], "reps")
"""

from typing import Dict, List, Optional, Tuple, Union
from .biomechanics import PullUpBiomechanicsAnalyzer
from .heatmap import MuscleHeatmapRenderer
from .dashboard_renderer import PullUpDashboardRenderer


class PullUpExerciseModule:
    """
    Modular, deterministic Pull-Up analysis engine.
    Accomplishes:
      1. Analysis & Metrics: rep counting, ROM, joint angles, velocity loss, form scores, 3-state fatigue.
      2. Heat Map Overlay: muscle temperature/fatigue mapped to silhouette polygons with thermal gradient.
      3. Dashboard UI Rendering: live HUD gauges, rep card, trajectory waveform, and session summaries.
    """

    def __init__(self,
                 height_m: float = 1.88,
                 mass_kg: float = 79.0,
                 fps: float = 30.0,
                 custom_bar_y: Optional[float] = None,
                 enable_thermal_overlay: bool = True,
                 enable_dashboard_hud: bool = True):
        self.height_m = height_m
        self.mass_kg = mass_kg
        self.fps = fps

        self.analyzer = PullUpBiomechanicsAnalyzer(
            height_m=height_m,
            mass_kg=mass_kg,
            fps=fps,
            custom_bar_y=custom_bar_y
        )
        self.heatmap_renderer = MuscleHeatmapRenderer(alpha=0.50)
        self.dashboard_renderer = PullUpDashboardRenderer()

        self.enable_thermal_overlay = enable_thermal_overlay
        self.enable_dashboard_hud = enable_dashboard_hud
        self.last_metrics: Optional[Dict] = None

    def update(self,
               landmarks: Union[List, Dict, 'np.ndarray'],
               timestamp_s: Optional[float] = None) -> Dict:
        """
        Processes a new frame of BlazePose landmarks.
        Returns live kinematic, fatigue, and rep metrics dictionary.
        """
        self.last_metrics = self.analyzer.process_frame(landmarks, timestamp_s=timestamp_s)
        return self.last_metrics

    def render(self, frame, landmarks: Optional[Union[List, Dict, 'np.ndarray']] = None):
        """
        Renders the thermal heatmap overlay and the live HUD dashboard onto the image frame.
        """
        if self.last_metrics is None:
            return frame

        parsed_lm = self.analyzer._extract_landmarks(landmarks) if landmarks is not None else None

        # Step 1: Render muscle thermal heat map on silhouette
        if self.enable_thermal_overlay and parsed_lm:
            frame = self.heatmap_renderer.render_overlay(
                frame, parsed_lm, self.analyzer.muscle_states
            )

        # Step 2: Render dashboard HUD, gauges, rep card, waveform
        if self.enable_dashboard_hud:
            frame = self.dashboard_renderer.render_live_dashboard(
                frame, self.last_metrics, parsed_lm
            )

        return frame

    def get_muscle_thermal_polygons(self,
                                   landmarks,
                                   width: int = 1280,
                                   height: int = 720) -> List[Dict]:
        """
        Returns structured 2D polygon geometries, RGB colors, and effort values
        for client-side rendering in WebGL, Canvas, or Android Compose.
        """
        parsed_lm = self.analyzer._extract_landmarks(landmarks)
        if not parsed_lm:
            return []
        return self.heatmap_renderer.compute_muscle_regions(
            parsed_lm, self.analyzer.muscle_states, width, height
        )

    def finish_set(self) -> Dict:
        """
        Completes the current set and returns detailed summary statistics.
        """
        return self.analyzer.get_session_summary()
