(ns build.create-index
  (:require
   [build.util :as u]))

(defn render [module-id->output-name]
  (str
   "<!doctype html>
<html lang=\"en-US\">
  <head>
    <meta charset=\"utf-8\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=0\">
    <meta name=\"theme-color\" content=\"#121212\">
    <meta name=\"app-version\" content=\"" (u/app-version) "\">
    <meta name=\"description\" content=\"Random player order selector.\">
    <title>Player order selector</title>
    <link rel=\"stylesheet\" href=\"" (u/asset "css/styles.css" module-id->output-name) "\">
    <link rel=\"icon\" href=\"" (u/asset "img/icon.svg" module-id->output-name) "\" type=\"image/svg+xml\">
    <link rel=\"apple-touch-icon\" href=\"" (u/asset "img/icon_192.png" module-id->output-name) "\">
    <link rel=\"manifest\" href=\"" (u/asset "manifest.json" module-id->output-name) "\">
  </head>
  <body>
    <div id=\"app\">
      <div class=\"game\">
        <canvas tabindex=\"1\">Your browser doesn't support canvas.</canvas>
        <div class=\"footer\">Version: " (u/app-version) "</div>
      </div>
    </div>
    <script src=\"" (u/asset "js/app.js" module-id->output-name) "\"></script>
    <script>app.core.init();</script>
  </body>
</html>"))
