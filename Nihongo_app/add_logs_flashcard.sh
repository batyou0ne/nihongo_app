#!/bin/bash
sed -i '' '1a\
import os\
private let flashLogger = Logger(subsystem: "com.batuhankeskn0.Nihongo-app", category: "Debug")
' Nihongo_app/Views/Learning/FlashcardSessionView.swift

sed -i '' 's/var body: some View {/var body: some View {\n        let _ = flashLogger.info("FlashcardSessionView rendering body. quizViewModel = \\(quizViewModel != nil ? "YES" : "NO")")/' Nihongo_app/Views/Learning/FlashcardSessionView.swift
