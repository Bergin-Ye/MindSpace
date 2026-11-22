<script setup>
import Icon from '../components/Icon.vue'
</script>

<template>
  <div class="auth">
    <section class="auth__aside">
      <RouterLink class="auth__brand" :to="{ name: 'login' }">
        MindSpace<span class="auth__dot" aria-hidden="true" />
      </RouterLink>

      <div class="auth__body">
        <p class="auth__question">“自动配置到底做了什么？”</p>

        <figure class="auth__cite">
          <blockquote>
            启动时读取候选配置类，再按条件注解决定哪些装配生效。并不是把所有东西都塞进容器。
            <span class="auth__ref">[1]</span>
          </blockquote>
          <figcaption class="auth__source">
            示例文档.md · 第 3 段 · 相似度 0.87
          </figcaption>
        </figure>

        <p class="auth__note">
          上传文档，解析正文，再让它回答问题。回答只引用你知识库里的段落，出处可以点开核对。
        </p>
      </div>

      <footer class="auth__foot">
        <span>MindSpace</span>
        <span class="auth__foot-dot" aria-hidden="true">·</span>
        <span>个人文档知识库</span>
      </footer>
    </section>

    <section class="auth__main">
      <div class="auth__form">
        <slot />
      </div>
      <p class="auth__legal">
        <Icon name="lock" :size="13" />
        文档与问答数据保存在你自己的后端服务中。
      </p>
    </section>
  </div>
</template>

<style scoped>
.auth {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(0, 1fr);
  gap: 14px;
  min-height: 100vh;
  padding: 14px;
}

.auth__aside,
.auth__main {
  position: relative;
  overflow: hidden;
  background-color: var(--glass);
  background-image: linear-gradient(
    155deg,
    rgba(255, 255, 255, 0.6),
    rgba(255, 255, 255, 0.14) 55%,
    rgba(255, 255, 255, 0.34)
  );
  -webkit-backdrop-filter: var(--glass-blur);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-island);
}

.auth__aside {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 32px 38px 26px;
}

.auth__aside::before {
  content: '';
  position: absolute;
  top: -32%;
  left: -24%;
  width: 92%;
  height: 82%;
  pointer-events: none;
  background: radial-gradient(
    closest-side,
    rgba(255, 136, 48, 0.3),
    rgba(255, 165, 92, 0.12) 58%,
    transparent 78%
  );
  filter: blur(6px);
}

.auth__brand {
  position: relative;
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  align-self: flex-start;
  color: var(--ink);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.035em;
}

.auth__brand:hover {
  color: var(--ink);
}

.auth__dot {
  width: 6px;
  height: 6px;
  background: var(--accent);
  border-radius: 50%;
  transform: translateY(-1px);
}

.auth__body {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 470px;
  margin: auto 0;
}

.auth__question {
  color: var(--ink);
  font-size: clamp(26px, 2.9vw, 36px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.04em;
  text-wrap: balance;
}

.auth__cite {
  margin: 0;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-left: 3px solid var(--accent);
  border-radius: 6px 18px 18px 6px;
  box-shadow: 0 10px 30px -22px rgba(24, 24, 28, 0.4);
  -webkit-backdrop-filter: blur(18px);
  backdrop-filter: blur(18px);
}

.auth__cite blockquote {
  margin: 0;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.8;
}

.auth__ref {
  margin-left: 2px;
  color: var(--accent-strong);
  font-size: 11px;
  font-weight: 600;
  vertical-align: super;
}

.auth__source {
  margin-top: 10px;
  color: var(--muted);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.auth__note {
  max-width: 38ch;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.8;
}

.auth__foot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 11px;
}

.auth__foot-dot {
  opacity: 0.6;
}

.auth__main {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28px;
  padding: 40px 26px;
}

.auth__form {
  width: min(360px, 100%);
}

.auth__legal {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
  font-size: 11px;
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
    padding: 10px;
  }

  .auth__aside {
    gap: 16px;
    padding: 22px 22px 24px;
    border-radius: 24px;
  }

  .auth__body {
    gap: 18px;
    margin: 4px 0 0;
  }

  .auth__question {
    font-size: 23px;
  }

  .auth__note,
  .auth__foot {
    display: none;
  }

  .auth__main {
    padding: 32px 22px 40px;
    border-radius: 24px;
  }
}
</style>
