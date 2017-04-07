jQuery.fn.textWidth = function(text, font) {
    if (!font) {
      font = "12px sans-serif";
    }
    if (!jQuery.fn.textWidth.fakeEl) {
      jQuery.fn.textWidth.fakeEl = jQuery('<span>').hide().appendTo(document.body);
    }
    jQuery.fn.textWidth.fakeEl.text(text || this.val() || this.text()).css('font', font || this.css('font'));
    return jQuery.fn.textWidth.fakeEl.width();
};
