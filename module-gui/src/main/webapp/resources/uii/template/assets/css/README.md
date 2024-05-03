# ZLB E-Pflicht CSS

ZLB E-Pflicht uses three `goobi workflow` plugins:

1. `goobi-plugin-rest-user-creation`
2. `goobi-plugin-dashboard-delivery`
3. `goobi-plugin-administration-deliveryManagement`

`1` and `2` are styled according to a custom ZLB-Theme. Both plugins use the `reduced_content_template.html` template. Therefore they share most of the CSS rules. However, some custom styles are imported separately into `2`. This import can be found on top of `plugin_dashboard_delivery.xhtml`.

Styles for `3` are based on [Bootstrap 3](https://getbootstrap.com/docs/3.3/). They can be found in the plugin itself: `/goobi-plugin-administration-deliveryManagement/src/main/resources/GUI/META-INF/resources/uii/css/index.css`.

Styles for `1` and `2` are based on [Bootstrap 5](https://getbootstrap.com). They can be found in `src`:

- `bootstrap.scss` defines which parts of BS 5 to include
- `custom.scss`: custom styles for `1` and `2`
- `dashboard-delivery.scss`: custom styles only used in `2`

## Build system

[sass](https://sass-lang.com/) is used to compile `scss` into css.
[postcss](https://postcss.org/) is used to prefix + minify css.

`postcss` is configured an run from a separate script: `runPostcss.js`.

### Usage
```sh
# Install sass, postcss (+ plugins), and bootstrap
npm install

# Develop: watch src and compile css into dist (sass only)
npm start

# Build: compile sass and transform css with postcss
npm run css:build
```
