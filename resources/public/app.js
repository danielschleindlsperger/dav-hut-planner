const btn = document.getElementById('add-stop')
btn.addEventListener('click', e => {
    e.preventDefault()
    const tourStops = document.getElementById('tour-stops')
    const cloneTarget = tourStops?.querySelector('input[type="text"]').parentNode
    const clone = cloneTarget.cloneNode(true)
    clone.querySelector('input').value = null
    tourStops.insertBefore(clone, btn)
})

document.addEventListener('click', e => {
    if (e.target && e.target.dataset.hasOwnProperty(
        "removeStop")) {
        e.preventDefault()
        e.target.parentNode.remove()
    }
})
