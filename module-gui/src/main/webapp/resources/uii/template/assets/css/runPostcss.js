// Node core
const fs = require('fs');

// Postcss
const postcss = require('postcss');
// Plugins
const autoprefixer = require('autoprefixer');
const cssnano = require('cssnano')({preset: 'default'});

const css = [
  {
    src: 'dist/bootstrap.css',
    dest: 'dist/bootstrap.min.css' 
  },
  {
    src: 'dist/custom.css',
    dest: 'dist/custom.min.css' 
  },
  {
    src: 'dist/dashboard-delivery.css',
    dest: 'dist/dashboard-delivery.min.css' 
  }
];

const processCss = (filePaths) => {
  const { src, dest } = filePaths;

  fs.readFile(src, (err, css) => {
    css || console.log(`Problem with ${src}, it is: >> ${css} <<`)
    css && postcss([autoprefixer, cssnano]) 
      .process(css, { from: src, to: dest })
      .then(result => {
        fs.writeFile(dest, result.css, () => true)
        if ( result.map ) {
          fs.writeFile(`${dest}.map`, result.map.toString(), () => true)
        }
      })
  })

}
css.map(filePaths => processCss(filePaths))
