import yanwittmann.*;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SiteBuilder {

    private final String siteOutDir;
    private final String sitePagesDir;
    private final String siteTemplateDir;
    private final String siteImagesDir;
    private final String templatePlaceTitle;
    private final String templatePlaceBody;
    private final String templatePlaceCss;
    private final String templatePlaceWebsiteTitle;
    private final String templatePlaceIcon;
    private final String templatePlaceMainPage;
    private final String templatePlaceContents;
    private final String templateInsert;
    private final String templateSubTitle;
    private final String templateMainTitle;
    private final String templateTextTitle;
    private final String templateTextParagraphIntro;
    private final String templateCss;
    private final String templateWebsiteTitle;
    private final String templateIcon;
    private final String templateSubTitleDefault;
    private final String templateMainCss;
    private final String templateInformationCss;
    private final String siteImageIcon;
    private final String siteTitleImage;
    private final String mainPageUrl;
    private final String regexLink;
    private final String regexLinkReplace;

    public SiteBuilder(Configuration configuration) {
        siteOutDir = configuration.getOrDefault("siteOutDir", "out\\site\\");
        sitePagesDir = configuration.getOrDefault("sitePagesDir", "res\\site\\pages\\");
        siteTemplateDir = configuration.getOrDefault("siteTemplateDir", "res\\site\\templates\\");
        siteImagesDir = configuration.getOrDefault("siteImagesDir", "res\\site\\img\\");

        templatePlaceTitle = configuration.getOrDefault("templatePlaceTitle", "BUILDER-PLACE-TITLE");
        templatePlaceBody = configuration.getOrDefault("templatePlaceBody", "BUILDER-PLACE-BODY");
        templatePlaceCss = configuration.getOrDefault("templatePlaceCss", "BUILDER-PLACE-CSS");
        templatePlaceWebsiteTitle = configuration.getOrDefault("templatePlaceWebsiteTitle", "BUILDER-PLACE-WEBSITE-TITLE");
        templatePlaceIcon = configuration.getOrDefault("templatePlaceIcon", "BUILDER-PLACE-ICON");
        templatePlaceMainPage = configuration.getOrDefault("templatePlaceMainPage", "BUILDER-PLACE-MAIN-PAGE");
        templatePlaceContents = configuration.getOrDefault("templatePlaceContents", "BUILDER-PLACE-CONTENTS");
        templateInsert = configuration.getOrDefault("templateInsert", "INSERT");
        templateSubTitle = configuration.getOrDefault("templateSubTitle", "<h1>INSERT</h1>");
        templateMainTitle = configuration.getOrDefault("templateMainTitle", "<h1>INSERT</h1>");
        templateTextTitle = configuration.getOrDefault("templateTextTitle", "<h4>INSERT</h4>");
        templateTextParagraphIntro = configuration.getOrDefault("templateTextParagraphIntro", "<p>");
        templateCss = configuration.getOrDefault("templateCss", "<link rel=\"stylesheet\" href=\"INSERT\" media=\"screen\">");
        templateWebsiteTitle = configuration.getOrDefault("templateWebsiteTitle", "<title>INSERT</title>");
        templateIcon = configuration.getOrDefault("templateIcon", "<img src=\"INSERT\" class=\"u-logo-image u-logo-image-1\" data-image-width=\"80.4586\">");

        templateSubTitleDefault = configuration.getOrDefault("templateSubTitleDefault", "Hauptthema");
        templateMainCss = configuration.getOrDefault("templateMainCss", "nicepage.css");
        templateInformationCss = configuration.getOrDefault("templateInformationCss", "information.css");

        siteImageIcon = configuration.getOrDefault("siteImageIcon", "itabiicon.png");
        siteTitleImage = configuration.getOrDefault("siteTitleImage", "227231823-carl-bosch-schule-Lef1.jpg");
        mainPageUrl = configuration.getOrDefault("mainPageUrl", "http://yanwittmann.de");

        regexLink = configuration.getOrDefault("regexLink", "\\[\\[href=([^|]+)\\|([^]]+)]]");
        regexLinkReplace = configuration.getOrDefault("regexLinkReplace", "<a href=\"$1\">$2</a>");
    }

    public void clearOldSite() {
        FileUtils.deleteDirectory(new File(siteOutDir));
    }

    private ArrayList<String> orderedPages;
    private final PageTreeBuilder pageTreeBuilder = new PageTreeBuilder();

    public void buildSite() {
        ArrayList<File> files = FileUtils.getFiles(new File(sitePagesDir));
        files.forEach(this::registerPage);
        informationPages.forEach(pageTreeBuilder::add);
        pageTreeBuilder.finish();
        orderedPages = pageTreeBuilder.getOrderedPages();

        ArrayList<String> template = FileUtils.readFileToArrayList(new File(siteTemplateDir + "informationtemplate.html"));
        files.forEach(siteFile -> buildInformationPage(siteFile, template));

        FileUtils.copyFile(new File(siteTemplateDir + "information.css"), new File(siteOutDir + "information.css"));
        FileUtils.copyFile(new File(siteTemplateDir + "Hauptseite.css"), new File(siteOutDir + "Hauptseite.css"));
        FileUtils.copyFile(new File(siteTemplateDir + "nicepage.css"), new File(siteOutDir + "nicepage.css"));
        FileUtils.copyFile(new File(siteImagesDir + siteImageIcon), new File(siteOutDir + "images\\" + siteImageIcon));
        FileUtils.copyFile(new File(siteImagesDir + siteTitleImage), new File(siteOutDir + "images\\" + siteTitleImage));
        buildMainPage();
    }

    private void buildMainPage() {
        ArrayList<String> template = FileUtils.readFileToArrayList(new File(siteTemplateDir + "Hauptseite.html"));

        HTMLListBuilder mainList = pageTreeBuilder.finish();

        LineBuilder generatedPage = new LineBuilder();
        for (String line : template) {
            String trimmedLine = line.trim();
            if (trimmedLine.equals(templatePlaceBody)) {
                generatedPage.append(generatedPage.toString());
            } else if (trimmedLine.equals(templatePlaceCss)) {
                generatedPage.append(templateCss.replace(templateInsert, templateMainCss));
                generatedPage.append(templateCss.replace(templateInsert, templateInformationCss));
            } else if (trimmedLine.equals(templatePlaceIcon)) {
                generatedPage.append(templateIcon.replace(templateInsert, "images\\" + siteImageIcon));
            } else if (line.contains(templatePlaceMainPage)) {
                generatedPage.append(line.replace(templatePlaceMainPage, mainPageUrl));
            } else if (line.contains(templatePlaceContents)) {
                generatedPage.append(line.replace(templatePlaceContents, mainList.toString()));
            } else {
                generatedPage.append(line);
            }
        }

        FileUtils.writeFile(new File(siteOutDir + "index.html"), optimizeGenerated(generatedPage.toString()));
    }

    private Pair<String, String> getSurroundingInfoPages(File page) {
        Pair<String, String> pair = new Pair<>();
        String lookingForPage = page.toString().replace(".txt", ".html").replace(sitePagesDir, "");
        for (int i = 0, orderedPagesSize = orderedPages.size(); i < orderedPagesSize; i++) {
            String orderedPage = orderedPages.get(i);
            if (orderedPage.contains(lookingForPage)) {
                if (i - 1 >= 0)
                    pair.setLeft(orderedPages.get(i - 1));
                if (i + 1 < orderedPagesSize)
                    pair.setRight(orderedPages.get(i + 1));
            }
        }
        if (pair.getLeft() == null)
            pair.setLeft("");
        if (pair.getRight() == null)
            pair.setRight("");
        return pair;
    }

    private void registerPage(File siteFile) {
        String path = prepareInformationPagePath(siteFile.getPath());
        String pageTitle = "Title";
        for (String line : FileUtils.readFileToArrayList(siteFile))
            if (line.startsWith("# ")) { //main title
                pageTitle = line.replace("# ", "");
                break;
            }
        pathToMainDirectory = IntStream.range(0, GeneralUtils.countOccurrences(path, "\\") + (path.equals(templateSubTitleDefault) ? 0 : 1))
                .mapToObj(i -> "..\\").collect(Collectors.joining());
        informationPages.add(new InformationPage(pageTitle, siteFile, path.replace(templateSubTitleDefault, "")));
    }

    private final ArrayList<InformationPage> informationPages = new ArrayList<>();
    private String pathToMainDirectory;

    private void buildInformationPage(File siteFile, ArrayList<String> template) {
        String path = prepareInformationPagePath(siteFile.getPath());
        String pageSubtitle = path.replace("\\", " >> ");
        String pageTitle = "Title";
        pathToMainDirectory = IntStream.range(0, GeneralUtils.countOccurrences(path, "\\") + (path.equals(templateSubTitleDefault) ? 0 : 1))
                .mapToObj(i -> "..\\").collect(Collectors.joining());

        LineBuilder generatedBody = new LineBuilder();
        boolean isCurrentlyTextOrImage = false;
        for (String line : FileUtils.readFileToArrayList(siteFile)) {
            if (line.startsWith("# ")) { //main title
                pageTitle = line.replace("# ", "");
            } else if (line.startsWith("## ")) { //title in text
                if (isCurrentlyTextOrImage) {
                    isCurrentlyTextOrImage = false;
                    generatedBody.append("</p>");
                }
                generatedBody.append(templateTextTitle.replace(templateInsert, line.replace("## ", "")));
            } else if (line.startsWith("img ")) { //image
                if (!isCurrentlyTextOrImage) {
                    isCurrentlyTextOrImage = true;
                    generatedBody.append(templateTextParagraphIntro);
                }
                generatedBody.append("<br>").append("<img style=\"margin-top:10px;\" src=\"" + prepareImageLink(line.replace("img ", "")) + "\"/><br>");
            } else if (line.length() > 0) { //regular text
                if (!isCurrentlyTextOrImage) {
                    isCurrentlyTextOrImage = true;
                    generatedBody.append(templateTextParagraphIntro);
                }
                generatedBody.append(prepareBodyText(line));
            }
        }

        LineBuilder generatedPage = new LineBuilder();
        for (String line : template) {
            String trimmedLine = line.trim();
            if (trimmedLine.equals(templatePlaceTitle)) {
                Pair<String, String> beforeNext = getSurroundingInfoPages(siteFile);
                beforeNext.setLeft(beforeNext.getLeft().replace("href=\"", "href=\"" + pathToMainDirectory)
                        .replaceAll("(.*)>(.+)</a>", "$1 title=\"$2\">$2</a>")
                        .replaceAll(">(.+)</a>", ">&lt;</a>"));
                beforeNext.setRight(beforeNext.getRight().replace("href=\"", "href=\"" + pathToMainDirectory)
                        .replaceAll("(.*)>(.+)</a>", "$1 title=\"$2\">$2</a>")
                        .replaceAll(">(.+)</a>", ">&gt;</a>"));
                generatedPage.append(templateMainTitle.replace(templateInsert, beforeNext.getLeft() + " &#160&#160&#160 " + pageTitle + " &#160&#160&#160 " + beforeNext.getRight()));
                generatedPage.append(templateSubTitle.replace(templateInsert, pageSubtitle));
            } else if (trimmedLine.equals(templatePlaceBody)) {
                generatedPage.append(generatedBody.toString());
            } else if (trimmedLine.equals(templatePlaceCss)) {
                generatedPage.append(templateCss.replace(templateInsert, pathToMainDirectory + templateMainCss));
                generatedPage.append(templateCss.replace(templateInsert, pathToMainDirectory + templateInformationCss));
            } else if (trimmedLine.equals(templatePlaceWebsiteTitle)) {
                generatedPage.append(templateWebsiteTitle.replace(templateInsert, pageTitle));
            } else if (trimmedLine.equals(templatePlaceIcon)) {
                generatedPage.append(templateIcon.replace(templateInsert, pathToMainDirectory + "images\\" + siteImageIcon));
            } else if (line.contains(templatePlaceMainPage)) {
                generatedPage.append(line.replace(templatePlaceMainPage, pathToMainDirectory + mainPageUrl));
            } else {
                generatedPage.append(line);
            }
        }

        System.out.println("Generated " + pageSubtitle + " / " + pageTitle);

        FileUtils.writeFile(new File(siteOutDir + path.replace(templateSubTitleDefault, "") + "\\" + siteFile.getName().replace(".txt", ".html")), optimizeGenerated(generatedPage.toString()));
    }

    private String prepareImageLink(String link) {
        if (link.contains("http"))
            return link;
        File image = new File(siteImagesDir + link);
        if (image.exists()) {
            File destination = new File(siteOutDir + "\\images\\" + link);
            FileUtils.makeDirectories(destination.getPath().replace(image.getName(), ""));
            FileUtils.copyFile(image, destination);
        } else System.out.println("Image does not exist: " + image.getPath());
        return (pathToMainDirectory + "\\images\\" + link).replaceAll("^\\\\", "");
    }

    private String prepareBodyText(String text) {
        return text.replaceAll(regexLink, regexLinkReplace);
    }

    private String prepareInformationPagePath(String path) {
        path = path.replaceAll(".*" + sitePagesDir.replace("\\", "\\\\") + "(.+)", "$1");
        return path.contains("\\") ? path.replaceAll("(.+)\\\\.+", "$1") : templateSubTitleDefault;
    }

    private String optimizeGenerated(String generated) {
        return generated.replaceAll("\n<br>", "<br>");
    }
}
