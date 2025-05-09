/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AIPatternsWebApplication {

	public static void main(String[] args) {
		Runtime r = Runtime.getRuntime();
		System.out.println("Runtime Data:");
		System.out.println("QuotesApplication: Active processors: " + r.availableProcessors());
		System.out.println("QuotesApplication: Total memory: " + r.totalMemory());
		System.out.println("QuotesApplication: Free memory: " + r.freeMemory());
		System.out.println("QuotesApplication: Max memory: " + r.maxMemory());

		SpringApplication.run(AIPatternsWebApplication.class, args);
	}

}
