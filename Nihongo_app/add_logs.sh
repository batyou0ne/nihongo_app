#!/bin/bash
sed -i '' '1a\
import os\
let logger = Logger(subsystem: "com.batuhankeskn0.Nihongo-app", category: "Debug")
' Nihongo_app/Views/Vocabulary/VocabularySessionView.swift

sed -i '' 's/var body: some View {/var body: some View {\n        let _ = logger.info("VocabularySessionView rendering body")/' Nihongo_app/Views/Vocabulary/VocabularySessionView.swift

sed -i '' 's/self.isLoading = false/logger.info("VocabularySessionView loadSession complete"); self.isLoading = false/' Nihongo_app/Views/Vocabulary/VocabularySessionView.swift
