package com.maxcape.accounthistory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class EventBatcher
{
	@Value
	@Builder(toBuilder = true)
	static class PendingBatch
	{
		String key;
		String eventType;
		int count;
		String firstAt;  // ISO-8601 UTC string
		String lastAt;   // ISO-8601 UTC string
		Map<String, Object> extraData;
	}

	private final Map<String, PendingBatch> pending = new LinkedHashMap<>();
	private final File storeFile;
	private final Gson gson;

	EventBatcher(File storeFile, Gson gson)
	{
		this.storeFile = storeFile;
		this.gson = gson;
	}

	synchronized void record(String key, String eventType, Map<String, Object> extraData)
	{
		String now = Instant.now().toString();
		PendingBatch existing = pending.get(key);
		if (existing == null)
		{
			log.debug("EventBatcher: new batch — key='{}' type='{}'", key, eventType);
			pending.put(key, PendingBatch.builder()
				.key(key)
				.eventType(eventType)
				.count(1)
				.firstAt(now)
				.lastAt(now)
				.extraData(extraData)
				.build());
		}
		else
		{
			int newCount = existing.getCount() + 1;
			log.debug("EventBatcher: incrementing batch — key='{}' count={}", key, newCount);
			pending.put(key, existing.toBuilder()
				.count(newCount)
				.lastAt(now)
				.extraData(extraData)
				.build());
		}
		persist();
	}

	synchronized List<PendingBatch> drain()
	{
		log.debug("EventBatcher: drain called — {} pending batch(es)", pending.size());
		if (pending.isEmpty())
		{
			return Collections.emptyList();
		}
		List<PendingBatch> result = new ArrayList<>(pending.values());
		pending.clear();
		persist();
		return result;
	}

	synchronized void load()
	{
		if (!storeFile.exists())
		{
			return;
		}
		try (FileReader reader = new FileReader(storeFile, StandardCharsets.UTF_8))
		{
			Type type = new TypeToken<Map<String, PendingBatch>>(){}.getType();
			Map<String, PendingBatch> loaded = gson.fromJson(reader, type);
			if (loaded != null)
			{
				pending.putAll(loaded);
				log.debug("EventBatcher: loaded {} pending batch(es) from disk", loaded.size());
			}
		}
		catch (IOException e)
		{
			log.debug("Failed to load pending batches from disk", e);
		}
	}

	private void persist()
	{
		try
		{
			storeFile.getParentFile().mkdirs();
			File tmp = new File(storeFile.getParentFile(), storeFile.getName() + ".tmp");
			try (FileWriter writer = new FileWriter(tmp, StandardCharsets.UTF_8))
			{
				gson.toJson(pending, writer);
			}
			Files.move(tmp.toPath(), storeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			log.debug("Failed to persist pending batches", e);
		}
	}
}
