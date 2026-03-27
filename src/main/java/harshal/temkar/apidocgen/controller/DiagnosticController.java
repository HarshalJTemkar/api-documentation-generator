package harshal.temkar.apidocgen.controller;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.apidocgen.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Diagnostic controller to help troubleshoot parsing issues.
 */
@RestController
@RequestMapping("/api/v1/diagnostics")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {

	/**
	 * Lists all controllers found in project.
	 */
	@PostMapping("/list-controllers")
	public ResponseEntity<Map<String, Object>> listControllers(@RequestBody Map<String, String> request) {
		String projectPath = request.get("projectPath");

		List<Path> javaFiles = FileUtil.findAllJavaFiles(projectPath);

		List<Map<String, String>> controllers = new ArrayList<>();

		for (Path javaFile : javaFiles) {
			String fileName = javaFile.getFileName().toString();
			if (fileName.contains("Controller")) {
				Map<String, String> controllerInfo = new HashMap<>();
				controllerInfo.put("fileName", fileName);
				controllerInfo.put("path", javaFile.toString());
				controllers.add(controllerInfo);
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("projectPath", projectPath);
		response.put("totalJavaFiles", javaFiles.size());
		response.put("controllersFound", controllers.size());
		response.put("controllers", controllers);

		return ResponseEntity.ok(response);
	}
}