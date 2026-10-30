<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${article.title}"/>
<c:set var="nav" value="guide"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="guide-article-container" id="guideArticle" data-ctx="${ctx}" data-article="<c:out value='${article.id}'/>">
    <div class="guide-crumbs">
        <a href="${ctx}/home">Trang chủ</a>
        <span class="guide-crumb-sep">/</span>
        <a href="${ctx}/guide">Cẩm nang khởi sự</a>
        <c:if test="${not empty category}">
            <span class="guide-crumb-sep">/</span>
            <a href="${ctx}/guide/search?category=<c:out value='${category.id}'/>"><c:out value="${category.title}"/></a>
        </c:if>
        <span class="guide-crumb-sep">/</span>
        <span class="guide-crumb-current"><c:out value="${article.title}"/></span>
    </div>

    <div class="guide-article-layout">
        <main class="guide-main-col">
            <header class="card guide-article-hero">
                <div class="guide-hero-meta">
                    <c:if test="${not empty category}">
                        <span class="guide-cat-badge"><c:out value="${category.title}"/></span>
                    </c:if>
                    <span class="guide-diff-badge"><c:out value="${article.difficulty}"/></span>
                    <span class="guide-time-badge">
                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                        ${article.readingTime} phút đọc
                    </span>
                    <c:choose>
                        <c:when test="${article.needsVerification}">
                            <span class="guide-tag-verify">
                                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                                Cần xác minh địa phương/sàn
                            </span>
                        </c:when>
                        <c:otherwise>
                            <span class="guide-tag-verified">
                                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                Căn cứ chính thức
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <h1 class="guide-doc-title"><c:out value="${article.title}"/></h1>

                <div class="guide-lead-box">
                    <div class="guide-lead-indicator"></div>
                    <p class="guide-doc-summary"><c:out value="${article.summary}"/></p>
                </div>

                <c:if test="${article.needsVerification}">
                    <div class="guide-alert-verify">
                        <div class="guide-alert-icon">
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                        </div>
                        <div class="guide-alert-body">
                            <strong>Lưu ý xác minh:</strong>
                            <c:if test="${not empty article.verifyNote}">
                                <span><c:out value="${article.verifyNote}"/></span>
                            </c:if>
                            <c:if test="${empty article.verifyNote}">
                                <span>Một số thông tin thủ tục hoặc biểu phí có thể thay đổi tùy thuộc địa phương hoặc chính sách sàn. Vui lòng đối chiếu với cơ quan thuế hoặc trung tâm hỗ trợ người bán trước khi áp dụng.</span>
                            </c:if>
                        </div>
                    </div>
                </c:if>

                <div class="guide-action-toolbar">
                    <button type="button" id="btnRead" data-on="${isRead ? 1 : 0}" class="guide-tool-btn ${isRead ? 'primary' : ''}">
                        <svg class="guide-btn-ico" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                        <span>${isRead ? 'Đã hoàn thành bài học' : 'Đánh dấu đã đọc'}</span>
                    </button>
                    <button type="button" id="btnBookmark" data-on="${isSaved ? 1 : 0}" class="guide-tool-btn ${isSaved ? 'primary' : ''}">
                        <svg class="guide-btn-ico" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                        <span>${isSaved ? 'Đã lưu trong thư viện' : 'Lưu lại để tra cứu'}</span>
                    </button>
                </div>
            </header>

            <c:if test="${not empty article.whyItMatters || not empty article.whenToDo}">
                <section class="card guide-section" id="context">
                    <div class="guide-context-grid">
                        <c:if test="${not empty article.whyItMatters}">
                            <div class="guide-context-item">
                                <div class="guide-item-title-row">
                                    <span class="guide-icon-pill icon-why">
                                        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                                    </span>
                                    <h3>Tại sao cần nắm rõ?</h3>
                                </div>
                                <p><c:out value="${article.whyItMatters}"/></p>
                            </div>
                        </c:if>
                        <c:if test="${not empty article.whenToDo}">
                            <div class="guide-context-item">
                                <div class="guide-item-title-row">
                                    <span class="guide-icon-pill icon-when">
                                        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 14 14"></polyline></svg>
                                    </span>
                                    <h3>Thời điểm thực hiện</h3>
                                </div>
                                <p><c:out value="${article.whenToDo}"/></p>
                            </div>
                        </c:if>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.overview}">
                <section class="card guide-section" id="overview">
                    <h2 class="guide-section-heading">
                        <span class="guide-heading-num">01</span>
                        Tổng Quan & Khái Niệm Cốt Lõi
                    </h2>
                    <div class="guide-prose">
                        <p><c:out value="${article.overview}"/></p>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.steps}">
                <section class="card guide-section" id="steps">
                    <h2 class="guide-section-heading">
                        <span class="guide-heading-num">02</span>
                        Quy Trình Triển Khai Từng Bước
                    </h2>
                    <div class="guide-stepper">
                        <c:forEach var="step" items="${article.steps}" varStatus="st">
                            <div class="guide-step-row">
                                <div class="guide-step-left">
                                    <div class="guide-step-bubble">${st.count}</div>
                                    <c:if test="${!st.last}"><div class="guide-step-line"></div></c:if>
                                </div>
                                <div class="guide-step-body">
                                    <div class="guide-step-text"><c:out value="${step}"/></div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.checklist}">
                <section class="card guide-section" id="checklist">
                    <div class="guide-checklist-head">
                        <div>
                            <h2 class="guide-section-heading" style="margin: 0 0 4px;">
                                <span class="guide-heading-num">03</span>
                                Checklist Hành Động Cần Hoàn Thành
                            </h2>
                            <p class="hint">Đánh dấu những việc bạn đã thực hiện để theo dõi tiến độ công việc</p>
                        </div>
                        <div class="guide-check-progress">
                            <span class="guide-check-badge"><span id="checkCount">${checkedCount}/${article.checklist.size()}</span> việc đã xong</span>
                        </div>
                    </div>
                    <div class="guide-checklist-list">
                        <c:forEach var="item" items="${article.checklist}" varStatus="st">
                            <label class="guide-check-row ${checked[st.index] ? 'is-done' : ''}">
                                <input type="checkbox" data-item="${st.index}" ${checked[st.index] ? 'checked' : ''}>
                                <span class="guide-check-content"><c:out value="${item}"/></span>
                            </label>
                        </c:forEach>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.costs || not empty article.commonMistakes}">
                <section class="card guide-section" id="costs-pitfalls">
                    <div class="guide-two-card-grid">
                        <c:if test="${not empty article.costs}">
                            <div class="guide-info-card costs-card">
                                <div class="guide-card-tag-head">
                                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
                                    <h3>Dự Toán Chi Phí & Ngân Sách</h3>
                                </div>
                                <div class="guide-info-text"><c:out value="${article.costs}"/></div>
                            </div>
                        </c:if>
                        <c:if test="${not empty article.commonMistakes}">
                            <div class="guide-info-card mistakes-card">
                                <div class="guide-card-tag-head">
                                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                                    <h3>Cạm Bẫy Người Mới Hay Mắc</h3>
                                </div>
                                <ul class="guide-bullet-list">
                                    <c:forEach var="m" items="${article.commonMistakes}">
                                        <li><c:out value="${m}"/></li>
                                    </c:forEach>
                                </ul>
                            </div>
                        </c:if>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.realCase}">
                <section class="card guide-section" id="case-study">
                    <h2 class="guide-section-heading">
                        <span class="guide-heading-num">04</span>
                        Tình Huống Ví Dụ Thực Tế
                    </h2>
                    <div class="guide-case-box">
                        <div class="guide-case-badge">Case Study Thực Tế</div>
                        <p><c:out value="${article.realCase}"/></p>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.faqs}">
                <section class="card guide-section" id="faqs">
                    <h2 class="guide-section-heading">
                        <span class="guide-heading-num">05</span>
                        Giải Đáp Câu Hỏi Thường Gặp
                    </h2>
                    <div class="guide-faq-accordion">
                        <c:forEach var="f" items="${article.faqs}">
                            <details class="guide-faq-node">
                                <summary class="guide-faq-summary">
                                    <span class="guide-faq-q"><c:out value="${f.question}"/></span>
                                    <svg class="guide-faq-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"></polyline></svg>
                                </summary>
                                <div class="guide-faq-answer">
                                    <p><c:out value="${f.answer}"/></p>
                                </div>
                            </details>
                        </c:forEach>
                    </div>
                </section>
            </c:if>

            <c:if test="${not empty article.sources}">
                <section class="card guide-section" id="sources">
                    <h2 class="guide-section-heading">
                        <span class="guide-heading-num">06</span>
                        Căn Cứ Pháp Lý & Nguồn Tham Chiếu
                    </h2>
                    <div class="guide-sources-grid">
                        <c:forEach var="src" items="${article.sources}">
                            <div class="guide-source-item">
                                <div class="guide-source-top">
                                    <span class="guide-source-name"><c:out value="${src.title}"/></span>
                                    <c:choose>
                                        <c:when test="${src.verified}">
                                            <span class="guide-source-pill verified">Chính thức</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="guide-source-pill unverified">Cần đối chiếu</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                                <c:if test="${src.verified && src.url.startsWith('https://')}">
                                    <a class="guide-source-link" href="<c:out value='${src.url}'/>" target="_blank" rel="noopener noreferrer">
                                        <span>Tra cứu văn bản gốc</span>
                                        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
                                    </a>
                                </c:if>
                            </div>
                        </c:forEach>
                    </div>
                </section>
            </c:if>
        </main>

        <aside class="guide-sidebar-col">
            <c:if test="${not empty article.actionUrl}">
                <div class="card guide-tinky-action-card">
                    <div class="guide-action-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>
                    </div>
                    <span class="guide-action-tag">Tích hợp phần mềm</span>
                    <h3 class="guide-action-title">Thực Hành Ngay Trên Tinky</h3>
                    <c:choose>
                        <c:when test="${actionOk}">
                            <p class="guide-action-desc"><c:out value="${article.actionLabel}"/></p>
                            <a class="btn primary guide-action-cta" href="${ctx}${article.actionUrl}">
                                <span>Mở tính năng ngay</span>
                                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <p class="guide-action-desc hint">Bước này yêu cầu quyền quản trị viên hoặc quản lý cửa hàng để truy cập.</p>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <div class="card guide-toc-card">
                <h4 class="guide-toc-title">
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"></line><line x1="8" y1="12" x2="21" y2="12"></line><line x1="8" y1="18" x2="21" y2="18"></line><line x1="3" y1="6" x2="3.01" y2="6"></line><line x1="3" y1="12" x2="3.01" y2="12"></line><line x1="3" y1="18" x2="3.01" y2="18"></line></svg>
                    Mục Lục Bài Đọc
                </h4>
                <nav class="guide-toc-nav">
                    <c:if test="${not empty article.whyItMatters}"><a href="#context">Bối cảnh & Thời điểm</a></c:if>
                    <c:if test="${not empty article.overview}"><a href="#overview">Tổng quan kiến thức</a></c:if>
                    <c:if test="${not empty article.steps}"><a href="#steps">Quy trình từng bước</a></c:if>
                    <c:if test="${not empty article.checklist}"><a href="#checklist">Checklist công việc</a></c:if>
                    <c:if test="${not empty article.costs || not empty article.commonMistakes}"><a href="#costs-pitfalls">Chi phí & Cạm bẫy</a></c:if>
                    <c:if test="${not empty article.realCase}"><a href="#case-study">Ví dụ thực tế</a></c:if>
                    <c:if test="${not empty article.faqs}"><a href="#faqs">Hỏi đáp thường gặp</a></c:if>
                    <c:if test="${not empty article.sources}"><a href="#sources">Căn cứ pháp lý</a></c:if>
                </nav>
            </div>

            <c:if test="${not empty related}">
                <div class="card guide-related-card">
                    <h4 class="guide-toc-title">
                        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
                        Bài Cùng Chuyên Mục
                    </h4>
                    <div class="guide-related-list">
                        <c:forEach var="r" items="${related}">
                            <a class="guide-related-item" href="${ctx}/guide/article?id=<c:out value='${r.id}'/>">
                                <span class="guide-related-name"><c:out value="${r.title}"/></span>
                                <span class="guide-related-meta">${r.readingTime} phút đọc · <c:out value="${r.difficulty}"/></span>
                            </a>
                        </c:forEach>
                    </div>
                </div>
            </c:if>
        </aside>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/guide.js?v=2" charset="UTF-8"></script>
</body>
</html>
