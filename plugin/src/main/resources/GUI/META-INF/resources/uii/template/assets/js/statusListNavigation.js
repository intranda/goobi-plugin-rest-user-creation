const statusListNavigation = (function () {

  // DEBUG
  const debug = true
  if (debug) console.log('## Status List Navigation ##')

  function init() {
    const statusList = document.querySelector('.statuslist');
    const listItems = statusList.querySelectorAll('.finished');
    listItems.forEach(item => item.addEventListener('click', () => {
      item.querySelector('button')?.click();
    }))
  } 

  return {
    init
  }

})()
