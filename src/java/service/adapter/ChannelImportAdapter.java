package service.adapter;

import java.util.Map;

public interface ChannelImportAdapter {

    String getChannel();

    Map<String, String> getColumnAliases();

    String parseStatus(String rawStatus);
}

