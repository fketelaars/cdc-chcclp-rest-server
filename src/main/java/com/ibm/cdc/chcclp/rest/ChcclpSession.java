package com.ibm.cdc.chcclp.rest;

import com.ibm.replication.cdc.scripting.EmbeddedScript;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import com.ibm.replication.cdc.scripting.Result;
import com.ibm.replication.cdc.scripting.ResultResultList;
import com.ibm.replication.cdc.scripting.ResultStringKeyValues;
import com.ibm.replication.cdc.scripting.ResultStringList;
import com.ibm.replication.cdc.scripting.ResultStringTable;
import com.ibm.replication.cdc.scripting.ResultStringValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single client CHCCLP session backed by one {@link EmbeddedScript} instance.
 *
 * <p>Each client obtains its own session so that server-connection state is fully isolated.
 * The {@link #execute(String)} method is {@code synchronized} because {@code EmbeddedScript}
 * is not thread-safe; a single session must only be used by one thread at a time.
 */
public class ChcclpSession {

    private final String id;
    private final EmbeddedScript script;
    private final Instant createdAt;
    private volatile Instant lastUsedAt;

    /**
     * Opens the underlying {@link EmbeddedScript}.
     * The caller is responsible for executing the {@code connect server} command afterwards.
     */
    public ChcclpSession(String id) throws EmbeddedScriptException {
        this.id = id;
        this.script = new EmbeddedScript();
        this.script.open();
        this.createdAt = Instant.now();
        this.lastUsedAt = createdAt;
    }

    /**
     * Executes a single CHCCLP command and returns its result as a JSON-friendly value.
     *
     * <p>The {@link Result} type is inspected and converted to a native Java structure
     * (List, Map, String, or {@code null}) so that Jackson can serialise it directly:
     * <ul>
     *   <li>{@code TABLE} → {@code List<Map<String,String>>}</li>
     *   <li>{@code LIST} → {@code List<String>}</li>
     *   <li>{@code KEY_VALUES} → {@code Map<String,String>}</li>
     *   <li>{@code VALUE} → {@code String}</li>
     *   <li>{@code RESULT_LIST} → {@code List<Object>} (each element converted recursively)</li>
     *   <li>{@code NULL} / no result → {@code null}</li>
     * </ul>
     *
     * @param command full CHCCLP command string, including the trailing semicolon
     * @return structured result, or {@code null} for commands that produce no output
     * @throws EmbeddedScriptException if CHCCLP reports an error
     */
    public synchronized Object execute(String command) throws EmbeddedScriptException {
        script.execute(command);
        lastUsedAt = Instant.now();
        return resultToObject(script.getResult());
    }

    /**
     * Closes the underlying {@link EmbeddedScript}. After this call the session
     * must not be used again.
     */
    public void close() {
        script.close();
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link Result} to a JSON-friendly Java object.
     */
    private static Object resultToObject(Result result) {
        if (result == null || result.getType() == Result.NULL) {
            return null;
        }
        return switch (result.getType()) {
            case Result.TABLE -> tableToList((ResultStringTable) result);
            case Result.LIST  -> listToList((ResultStringList) result);
            case Result.KEY_VALUES -> keyValuesToMap((ResultStringKeyValues) result);
            case Result.VALUE -> ((ResultStringValue) result).getValue();
            case Result.RESULT_LIST -> resultListToList((ResultResultList) result);
            default -> null;
        };
    }

    private static List<Map<String, String>> tableToList(ResultStringTable table) {
        List<Map<String, String>> rows = new ArrayList<>(table.getRowCount());
        for (int r = 0; r < table.getRowCount(); r++) {
            Map<String, String> row = new LinkedHashMap<>(table.getColumnCount());
            for (int c = 0; c < table.getColumnCount(); c++) {
                row.put(normaliseKey(table.getColumnAt(c)), table.getValueAt(r, c));
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> listToList(ResultStringList list) {
        List<String> items = new ArrayList<>(list.getRowCount());
        for (int i = 0; i < list.getRowCount(); i++) {
            items.add(list.getValueAt(i));
        }
        return items;
    }

    private static Map<String, String> keyValuesToMap(ResultStringKeyValues kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : kv.getKeys()) {
            map.put(normaliseKey(key), kv.getValue(key));
        }
        return map;
    }

    /** Lowercases a CHCCLP column/key name and replaces spaces with underscores. */
    private static String normaliseKey(String key) {
        return key.toLowerCase().replace(' ', '_');
    }

    private static List<Object> resultListToList(ResultResultList resultList) {
        List<Object> items = new ArrayList<>(resultList.getResultsCount());
        for (int i = 0; i < resultList.getResultsCount(); i++) {
            items.add(resultToObject(resultList.getResult(i)));
        }
        return items;
    }
}
