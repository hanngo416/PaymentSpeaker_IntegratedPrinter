<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Cẩm Nang Khởi Sự Kinh Doanh"/>
<c:set var="nav" value="guide"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="guide-hub">
    <div class="guide-banner">
        <div class="guide-banner-content">
            <h1>Cẩm Nang Khởi Sự Kinh Doanh Toàn Diện</h1>
            <p class="guide-banner-desc">Hệ thống bài học, quy trình vận hành thực tế và checklist từng bước giúp người mới bắt đầu từ số 0 tự tin mở quán, bán hàng online, tuân thủ pháp luật thuế và mở rộng đa kênh.</p>

            <form method="get" action="${ctx}/guide/search" class="guide-search-box">
                <div class="guide-search-input-wrap">
                    <svg class="guide-search-ico" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                    <input type="text" name="q" maxlength="100" value="<c:out value='${q}'/>" placeholder="Tìm kiếm nhanh: ngưỡng thuế 1 tỷ, phí sàn Shopee, TikTok Shop, hóa đơn điện tử...">
                    <input type="hidden" name="model" value="<c:out value='${model}'/>">
                    <input type="hidden" name="category" value="<c:out value='${category}'/>">
                </div>
                <button type="submit" class="btn primary guide-search-btn">Tìm kiếm</button>
                <c:if test="${filtered}">
                    <a class="btn guide-reset-btn" href="${ctx}/guide">Xem toàn bộ giáo trình</a>
                </c:if>
            </form>

            <div class="guide-filter-pills">
                <span class="guide-filter-label">Mô hình:</span>
                <a class="guide-chip ${empty model && !savedOnly ? 'active' : ''}" href="${ctx}/guide/search?q=<c:out value='${q}'/>&category=<c:out value='${category}'/>">Tất cả</a>
                <a class="guide-chip ${model == 'OFFLINE_STORE' ? 'active' : ''}" href="${ctx}/guide/search?q=<c:out value='${q}'/>&category=<c:out value='${category}'/>&model=OFFLINE_STORE">Quán ăn / Cửa hàng</a>
                <a class="guide-chip ${model == 'SHOPEE_SELLER' ? 'active' : ''}" href="${ctx}/guide/search?q=<c:out value='${q}'/>&category=<c:out value='${category}'/>&model=SHOPEE_SELLER">Shopee</a>
                <a class="guide-chip ${model == 'TIKTOK_SELLER' ? 'active' : ''}" href="${ctx}/guide/search?q=<c:out value='${q}'/>&category=<c:out value='${category}'/>&model=TIKTOK_SELLER">TikTok Shop</a>
                <a class="guide-chip ${model == 'OMNICHANNEL' ? 'active' : ''}" href="${ctx}/guide/search?q=<c:out value='${q}'/>&category=<c:out value='${category}'/>&model=OMNICHANNEL">Bán Đa Kênh</a>
                <a class="guide-chip guide-chip-saved ${savedOnly ? 'active' : ''}" href="${ctx}/guide/search?saved=1">
                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                    Đã lưu
                </a>
            </div>
        </div>

        <div class="guide-progress-widget">
            <div class="guide-progress-header">
                <span class="guide-widget-title">Tiến Độ Của Bạn</span>
                <span class="guide-widget-pct">${percent}%</span>
            </div>
            <div class="guide-progress-track">
                <div class="guide-progress-fill" style="width: ${percent}%;"></div>
            </div>
            <div class="guide-progress-stats">
                <span>Đã học <strong>${readCount}</strong> / ${totalCount} bài học</span>
            </div>
            <c:if test="${not empty nextArticle}">
                <div class="guide-next-box">
                    <span class="guide-next-hint">Bài học tiếp theo:</span>
                    <a class="guide-next-title" href="${ctx}/guide/article?id=<c:out value='${nextArticle.id}'/>"><c:out value="${nextArticle.title}"/></a>
                    <a class="guide-next-link" href="${ctx}/guide/article?id=<c:out value='${nextArticle.id}'/>">
                        <span>Vào học ngay</span>
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                    </a>
                </div>
            </c:if>
        </div>
    </div>

    <c:choose>
        <c:when test="${not available}">
            <div class="card guide-empty-state">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                <h3>Chưa có dữ liệu bài viết</h3>
                <p class="hint">Kho tri thức đang được chuẩn bị. Vui lòng liên hệ quản trị viên để kiểm tra tệp business_docs.json.</p>
            </div>
        </c:when>

        <c:when test="${filtered}">
            <div class="guide-section-head">
                <div>
                    <h2>Kết quả lọc (${articles.size()} bài viết)</h2>
                    <p class="guide-subhead">Danh sách bài học phù hợp với từ khóa hoặc mô hình bạn đã chọn</p>
                </div>
                <a class="btn" href="${ctx}/guide">Quay lại toàn bộ giáo trình</a>
            </div>

            <c:choose>
                <c:when test="${empty articles}">
                    <div class="card guide-empty-state">
                        <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        <h3>Không tìm thấy bài viết nào</h3>
                        <p class="hint">Thử tìm kiếm với từ khóa khác hoặc <a href="${ctx}/guide">xóa bộ lọc</a> để xem lại toàn bộ bài viết.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card guide-curriculum-card">
                        <div class="guide-lesson-list">
                            <c:forEach var="a" items="${articles}" varStatus="st">
                                <a class="guide-lesson-row" href="${ctx}/guide/article?id=<c:out value='${a.id}'/>">
                                    <div class="guide-lesson-num ${read[a.id] ? 'is-read' : ''}">
                                        <c:choose>
                                            <c:when test="${read[a.id]}">
                                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                            </c:when>
                                            <c:otherwise>${st.count < 10 ? '0' : ''}${st.count}</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="guide-lesson-info">
                                        <div class="guide-lesson-title-row">
                                            <span class="guide-lesson-cat"><c:out value="${categoryMap[a.categoryId].title}"/></span>
                                            <span class="guide-lesson-title"><c:out value="${a.title}"/></span>
                                        </div>
                                        <p class="guide-lesson-excerpt"><c:out value="${a.summary}"/></p>
                                    </div>
                                    <div class="guide-lesson-meta">
                                        <c:if test="${a.needsVerification}">
                                            <span class="guide-tag-verify">Cần xác minh</span>
                                        </c:if>
                                        <span class="guide-diff-badge"><c:out value="${a.difficulty}"/></span>
                                        <span class="guide-time-badge">${a.readingTime} phút</span>
                                        <c:if test="${saved[a.id]}">
                                            <span class="guide-flag-saved" title="Đã lưu">
                                                <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                                            </span>
                                        </c:if>
                                        <svg class="guide-lesson-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
                                    </div>
                                </a>
                            </c:forEach>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:when>

        <c:otherwise>
            <div class="guide-quick-nav">
                <span class="guide-quick-label">Nhảy nhanh đến:</span>
                <c:forEach var="cat" items="${categories}" varStatus="st">
                    <a class="guide-quick-pill" href="#cat-${cat.id}">
                        <span class="guide-quick-num">0${st.count}</span>
                        <span><c:out value="${cat.title}"/></span>
                    </a>
                </c:forEach>
            </div>

            <div class="guide-curriculum-container">
                <c:forEach var="cat" items="${categories}" varStatus="st">
                    <section class="card guide-curriculum-card" id="cat-${cat.id}">
                        <header class="guide-curriculum-head">
                            <div class="guide-curriculum-head-left">
                                <span class="guide-stage-tag">CHẶNG 0${st.count}</span>
                                <h2 class="guide-curriculum-title"><c:out value="${cat.title}"/></h2>
                                <p class="guide-curriculum-desc"><c:out value="${cat.description}"/></p>
                            </div>
                            <div class="guide-curriculum-head-right">
                                <div class="guide-stage-stat">
                                    <span>Đã học <strong>${empty catRead[cat.id] ? 0 : catRead[cat.id]}</strong> / ${empty catTotal[cat.id] ? 0 : catTotal[cat.id]} bài</span>
                                    <c:set var="totalC" value="${empty catTotal[cat.id] ? 1 : catTotal[cat.id]}"/>
                                    <c:set var="readC" value="${empty catRead[cat.id] ? 0 : catRead[cat.id]}"/>
                                    <div class="guide-mini-bar" style="width: 110px;">
                                        <div class="guide-mini-fill" style="width: ${totalC == 0 ? 0 : (readC * 100 / totalC)}%;"></div>
                                    </div>
                                </div>
                            </div>
                        </header>

                        <div class="guide-lesson-list">
                            <c:forEach var="a" items="${catArticles[cat.id]}" varStatus="ast">
                                <a class="guide-lesson-row" href="${ctx}/guide/article?id=<c:out value='${a.id}'/>">
                                    <div class="guide-lesson-num ${read[a.id] ? 'is-read' : ''}">
                                        <c:choose>
                                            <c:when test="${read[a.id]}">
                                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                            </c:when>
                                            <c:otherwise>${ast.count < 10 ? '0' : ''}${ast.count}</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="guide-lesson-info">
                                        <h3 class="guide-lesson-title"><c:out value="${a.title}"/></h3>
                                        <p class="guide-lesson-excerpt"><c:out value="${a.summary}"/></p>
                                    </div>
                                    <div class="guide-lesson-meta">
                                        <c:if test="${a.needsVerification}">
                                            <span class="guide-tag-verify">Cần xác minh</span>
                                        </c:if>
                                        <span class="guide-diff-badge"><c:out value="${a.difficulty}"/></span>
                                        <span class="guide-time-badge">${a.readingTime} phút</span>
                                        <c:if test="${saved[a.id]}">
                                            <span class="guide-flag-saved" title="Đã lưu">
                                                <svg viewBox="0 0 24 24" width="13" height="13" fill="currentColor"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                                            </span>
                                        </c:if>
                                        <svg class="guide-lesson-arrow" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"></polyline></svg>
                                    </div>
                                </a>
                            </c:forEach>
                        </div>
                    </section>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
