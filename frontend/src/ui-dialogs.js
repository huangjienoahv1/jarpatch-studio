/**
 * 内置主题化确认对话框模块。
 * <p>
 * 彻底替代浏览器原生 window.confirm，提供深浅双主题统一、支持结构化详情卡片、
 * 滚动目录清单和多语义按钮的异步确认能力，兼容 Electron 22 (Chromium 108) 及现代版本。
 * </p>
 *
 * @author 黄杰
 */
(function initializeUiDialogs(global) {
  'use strict';

  const DIALOG_ID = 'confirmDialog';
  const TONE_INFO = 'info';
  const TONE_WARN = 'warn';
  const TONE_DANGER = 'danger';
  const VARIANT_PRIMARY = 'primary';
  const VARIANT_DANGER = 'danger';
  const VARIANT_SECONDARY = 'secondary';
  const DEFAULT_CONFIRM_TITLE = '请确认操作';
  const DEFAULT_CANCEL_LABEL = '取消';
  const DEFAULT_OK_LABEL = '确定';

  const TONE_ICON_MAP = {
    info: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>',
    warn: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m10.29 3.86-8.48 14.7A2 2 0 0 0 3.53 21h16.94a2 2 0 0 0 1.72-2.44l-8.48-14.7a2 2 0 0 0-3.42 0Z"/><path d="M12 9v4M12 17h.01"/></svg>',
    danger: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6M9 9l6 6"/></svg>'
  };

  let activeResolver = null;

  /**
   * 获取确认弹窗及其内部静态节点。
   *
   * @returns {object|null} DOM 节点集合
   */
  function getDialogElements() {
    const dialog = document.getElementById(DIALOG_ID);
    if (!dialog) {
      return null;
    }
    return {
      dialog,
      icon: document.getElementById('confirmDialogIcon'),
      title: document.getElementById('confirmDialogTitle'),
      message: document.getElementById('confirmDialogMessage'),
      details: document.getElementById('confirmDialogDetails'),
      list: document.getElementById('confirmDialogList'),
      footer: document.getElementById('confirmDialogFooter'),
      closeBtn: document.getElementById('confirmDialogCloseBtn')
    };
  }

  /**
   * 将按钮风格枚举映射为对应 CSS 类名。
   *
   * @param {string} variant 按钮语义类型
   * @returns {string} 样式类名
   */
  function resolveButtonClass(variant) {
    if (variant === VARIANT_PRIMARY) {
      return 'primary-action confirm-btn-primary';
    }
    if (variant === VARIANT_DANGER) {
      return 'tool-btn confirm-btn-danger';
    }
    return 'tool-btn';
  }

  /**
   * 渲染结构化键值详情区。
   *
   * @param {HTMLElement} container 详情容器
   * @param {Array<{label: string, value: string}>} items 键值项
   */
  function renderDetails(container, items) {
    container.replaceChildren();
    if (!Array.isArray(items) || !items.length) {
      container.hidden = true;
      return;
    }
    for (const item of items) {
      const row = document.createElement('div');
      row.className = 'confirm-detail-row';
      const label = document.createElement('span');
      label.className = 'confirm-detail-label';
      label.textContent = item.label || '';
      const value = document.createElement('strong');
      value.className = 'confirm-detail-value';
      value.textContent = item.value == null ? '' : String(item.value);
      row.appendChild(label);
      row.appendChild(value);
      container.appendChild(row);
    }
    container.hidden = false;
  }

  /**
   * 渲染可滚动条目清单（适用于孤立工作区等多项确认场景）。
   *
   * @param {HTMLElement} container 清单容器
   * @param {Array<{title: string, meta: string}>} items 条目数组
   */
  function renderList(container, items) {
    container.replaceChildren();
    if (!Array.isArray(items) || !items.length) {
      container.hidden = true;
      return;
    }
    for (const item of items) {
      const card = document.createElement('div');
      card.className = 'confirm-list-item';
      const title = document.createElement('strong');
      title.textContent = item.title || '';
      const meta = document.createElement('span');
      meta.textContent = item.meta || '';
      card.appendChild(title);
      card.appendChild(meta);
      container.appendChild(card);
    }
    container.hidden = false;
  }

  /**
   * 打开主题化内置确认弹窗并等待用户选择。
   *
   * @param {object} options 弹窗配置（title, message, tone, details, listItems, actions, cancelValue）
   * @returns {Promise<any>} 用户点击对应按钮的 value，取消或按 Esc 时返回 cancelValue
   */
  function confirm(options = {}) {
    const refs = getDialogElements();
    if (!refs) {
      return Promise.resolve(false);
    }
    if (activeResolver) {
      const previous = activeResolver;
      activeResolver = null;
      previous(null);
    }

    const tone = options.tone === TONE_DANGER || options.tone === TONE_WARN ? options.tone : TONE_INFO;
    const cancelValue = Object.prototype.hasOwnProperty.call(options, 'cancelValue')
      ? options.cancelValue
      : false;
    const actions = Array.isArray(options.actions) && options.actions.length
      ? options.actions
      : [
        { value: false, label: DEFAULT_CANCEL_LABEL, variant: VARIANT_SECONDARY },
        { value: true, label: DEFAULT_OK_LABEL, variant: tone === TONE_DANGER ? VARIANT_DANGER : VARIANT_PRIMARY }
      ];

    refs.icon.className = `confirm-tone-icon tone-${tone}`;
    refs.icon.innerHTML = TONE_ICON_MAP[tone] || TONE_ICON_MAP.info;
    refs.title.textContent = options.title || DEFAULT_CONFIRM_TITLE;
    refs.message.textContent = options.message || '';
    refs.message.hidden = !options.message;
    renderDetails(refs.details, options.details);
    renderList(refs.list, options.listItems);

    return new Promise((resolve) => {
      let settled = false;
      const finish = (value) => {
        if (settled) {
          return;
        }
        settled = true;
        activeResolver = null;
        refs.dialog.removeEventListener('cancel', handleCancel);
        refs.dialog.removeEventListener('click', handleBackdropClick);
        refs.closeBtn.removeEventListener('click', handleCloseClick);
        if (refs.dialog.open) {
          refs.dialog.close();
        }
        resolve(value);
      };

      const handleCancel = (event) => {
        event.preventDefault();
        finish(cancelValue);
      };

      const handleCloseClick = () => {
        finish(cancelValue);
      };

      const handleBackdropClick = (event) => {
        if (event.target === refs.dialog) {
          finish(cancelValue);
        }
      };

      activeResolver = finish;
      refs.footer.replaceChildren();
      let focusTarget = null;
      for (const action of actions) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = resolveButtonClass(action.variant);
        button.textContent = action.label;
        button.addEventListener('click', () => finish(action.value));
        refs.footer.appendChild(button);
        if (action.autofocus || (!focusTarget && action.variant !== VARIANT_SECONDARY)) {
          focusTarget = button;
        }
      }

      refs.dialog.addEventListener('cancel', handleCancel);
      refs.dialog.addEventListener('click', handleBackdropClick);
      refs.closeBtn.addEventListener('click', handleCloseClick);
      refs.dialog.showModal();
      if (focusTarget) {
        focusTarget.focus();
      }
    });
  }

  global.jarPatchDialogs = Object.freeze({
    confirm,
    TONE_INFO,
    TONE_WARN,
    TONE_DANGER,
    VARIANT_PRIMARY,
    VARIANT_DANGER,
    VARIANT_SECONDARY
  });
}(window));
