package com.assessment.quest.part1.utils;

import com.assessment.quest.part1.model.BLSFileMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.assessment.quest.part1.utils.DateUtils.parseDateTimeToEpochMillis;

public class HtmlParser {

    private static final Pattern LINE_PATTERN = Pattern.compile("\\s*(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(\\d{1,2}:\\d{2}\\s+[AP]M)\\s+(\\d+)\\s*");

    public static List<BLSFileMetadata> parse(String html) {
        List<BLSFileMetadata> files = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements anchors = doc.select("pre a[href]");

        for (Element a : anchors) {
            String fileName = a.text().trim();
            if ("[To Parent Directory]".equals(fileName)) {
                continue;
            }

            String meta = findPrecedingText(a);
            if (meta == null || meta.isBlank()) {
                continue;
            }

            Matcher m = LINE_PATTERN.matcher(meta);
            if (!m.find()) {
                continue;
            }

            String dateStr = m.group(1);              // e.g., 9/4/2025
            String timeStr = m.group(2);              // e.g., 8:30 AM
            long size = Long.parseLong(m.group(3));   // e.g., 102

            long lastModifiedEpochMillis = parseDateTimeToEpochMillis(dateStr, timeStr);

            files.add(BLSFileMetadata.builder()
                    .name(fileName)
                    .size(size)
                    .lastModified(lastModifiedEpochMillis)
                    .build());
        }

        return files;
    }

    private static String findPrecedingText(Element anchor) {
        Node n = anchor.previousSibling();
        while (n != null) {
            if (n instanceof TextNode) {
                String text = ((TextNode) n).text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
            n = n.previousSibling();
        }
        return null;
    }

}
