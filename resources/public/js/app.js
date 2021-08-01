(() => {
    // add stop
    const btn = document.getElementById('add-stop')
    btn.addEventListener('click', e => {
        e.preventDefault()
        const tourStops = document.getElementById('tour-stops')
        const cloneTarget = tourStops?.querySelector('select').parentNode
        const clone = cloneTarget.cloneNode(true)
        clone.querySelector('select').selected = null
        clone.querySelector('.nice-select')?.remove()
        tourStops.insertBefore(clone, btn)
        initializeCombobox(clone.querySelector('select'))
    })

    // remove stop
    document.addEventListener('click', e => {
        if (e.target && e.target.dataset.hasOwnProperty(
            "removeStop")) {
            e.preventDefault()
            e.target.parentNode.remove()
        }
    })


    // initialize comboboxes initially
    const comboboxes = document.querySelectorAll('select')
    for (const combobox of comboboxes) {
        initializeCombobox(combobox)
    }

    // uses https://bluzky.github.io/nice-select2/
    function initializeCombobox(selectEl) {
        window.NiceSelect.bind(selectEl, {searchable: true});
    }
})()
