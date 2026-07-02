/*
 * Copyright (C) 2026 DevEmperor (Dictate)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.data.prompts

import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/**
 * Turns a user's own [PromptModel] into a "contribute to the community library" GitHub pull request,
 * opened straight from the app (issue #105).
 *
 * There is deliberately no backend: contributing is a GitHub deep link that pre-fills the web file
 * editor with the prompt as a JSON submission file. When a non-collaborator opens it, GitHub forks the
 * repo automatically and the "Propose new file" button creates the PR. The only requirement on the
 * user's side is a (free) GitHub account — no fork ceremony, no local git, no server.
 */
object PromptLibraryContribution {

    /**
     * Builds the pre-filled GitHub "new file" URL for [model]. The submission file is a single
     * [PromptLibraryEntry] wrapped as `{"version":1,"prompts":[…]}` so the same schema/validation the
     * hosted library uses applies to submissions too. [author] (optional) is embedded as attribution.
     */
    fun buildSubmissionUrl(model: PromptModel, author: String? = null): String {
        val slug = slugify(model.name)
        val entry = JSONObject()
            .put("id", slug)
            .put("name", model.name.orEmpty().trim())
            .put("prompt", model.prompt.orEmpty().trim())
            .put("requiresSelection", model.requiresSelection)
            .put("autoApply", model.autoApply)
        if (!author.isNullOrBlank()) entry.put("author", author.trim())
        val doc = JSONObject()
            .put("version", 1)
            .put("prompts", org.json.JSONArray().put(entry))
        val json = doc.toString(2)
        val encoded = URLEncoder.encode(json, "UTF-8").replace("+", "%20")
        val fileName = "${slug.ifBlank { "prompt" }}.json"
        return PromptLibraryCatalog.newFileUrl(fileName, encoded)
    }

    /** ASCII, lowercase, dash-separated slug from a prompt name, safe as a file name / catalog id. */
    fun slugify(name: String?): String {
        val base = name.orEmpty().lowercase(Locale.ROOT).trim()
        val slug = buildString {
            var lastDash = false
            for (ch in base) {
                if (ch in 'a'..'z' || ch in '0'..'9') {
                    append(ch); lastDash = false
                } else if (!lastDash && isNotEmpty()) {
                    append('-'); lastDash = true
                }
            }
        }.trim('-')
        return slug.take(48)
    }
}
