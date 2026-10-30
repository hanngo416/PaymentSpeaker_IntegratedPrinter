<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Cẩm Nang Khởi Sự Kinh Doanh"/>
<c:set var="nav" value="guide"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="guide-hub">
    <div class="guide-banner">
        <div class="guide-banner-content">
            <div class="guide-badge-pill">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"></path></svg>
                <span>Kiến thức thực chiến 2026 · Chuẩn hóa theo quy định mới</span>
            </div>
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
                    <a class="btn guide-reset-btn" href="${ctx}/guide">Xóa bộ lọc</a>
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
                <span class="guide-widget-title">Tiến Độ Học Tập</span>
                <span class="guide-widget-pct">${percent}%</span>
            </div>
            <div class="guide-progress-track">
                <div class="guide-progress-fill" style="width: ${percent}%;"></div>
            </div>
            <div class="guide-progress-stats">
                <span>Đã hoàn thành <strong>${readCount}</strong> / ${totalCount} bài</span>
            </div>
            <c:if test="${not empty nextArticle}">
                <div class="guide-next-box">
                    <span class="guide-next-hint">Bài học tiếp theo cho bạn:</span>
                    <a class="guide-next-title" href="${ctx}/guide/article?id=<c:out value='${nextArticle.id}'/>"><c:out value="${nextArticle.title}"/></a>
                    <a class="guide-next-link" href="${ctx}/guide/article?id=<c:out value='${nextArticle.id}'/>">
                        <span>Học tiếp ngay</span>
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
        <c:otherwise>
            <div class="guide-section-head">
                <div>
                    <h2>Lộ Trình Khởi Sự 6 Bước</h2>
                    <p class="guide-subhead">Trang bị kiến thức theo trình tự bài bản từ ý tưởng, pháp lý đến vận hành và mở rộng</p>
                </div>
            </div>

            <div class="guide-roadmap-grid">
                <c:forEach var="cat" items="${categories}" varStatus="st">
                    <a class="guide-roadmap-card ${category == cat.id ? 'active' : ''}" href="${ctx}/guide/search?category=<c:out value='${cat.id}'/>&model=<c:out value='${model}'/>">
                        <div class="guide-card-top">
                            <span class="guide-step-tag">CHẶNG 0${st.count}</span>
                            <div class="guide-step-count">
                                <strong>${empty catRead[cat.id] ? 0 : catRead[cat.id]}</strong>/${empty catTotal[cat.id] ? 0 : catTotal[cat.id]}
                            </div>
                        </div>
                        <h3 class="guide-step-title"><c:out value="${cat.title}"/></h3>
                        <p class="guide-step-desc"><c:out value="${cat.description}"/></p>
                        <div class="guide-mini-bar">
                            <c:set var="totalC" value="${empty catTotal[cat.id] ? 1 : catTotal[cat.id]}"/>
                            <c:set var="readC" value="${empty catRead[cat.id] ? 0 : catRead[cat.id]}"/>
                            <div class="guide-mini-fill" style="width: ${totalC == 0 ? 0 : (readC * 100 / totalC)}%;"></div>
                        </div>
                    </a>
                </c:forEach>
            </div>

            <div class="guide-section-head guide-articles-head">
                <div>
                    <h2>
                        <c:choose>
                            <c:when test="${filtered}">Kết quả tìm kiếm (${articles.size()} bài viết)</c:when>
                            <c:otherwise>Toàn Bộ Bài Hướng Dẫn (${articles.size()} bài)</c:otherwise>
                        </c:choose>
                    </h2>
                    <c:if test="${filtered}">
                        <p class="guide-subhead">Hiển thị bài viết phù hợp với tiêu chí lọc của bạn</p>
                    </c:if>
                </div>
            </div>

            <c:choose>
                <c:when test="${empty articles}">
                    <div class="card guide-empty-state">
                        <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        <h3>Không tìm thấy bài viết phù hợp</h3>
                        <p class="hint">Thử tìm kiếm với từ khóa khác hoặc <a href="${ctx}/guide">xóa bộ lọc</a> để xem lại toàn bộ bài viết.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="guide-article-grid">
                        <c:forEach var="a" items="${articles}">
                            <a class="guide-article-card" href="${ctx}/guide/article?id=<c:out value='${a.id}'/>">
                                <div class="guide-card-header">
                                    <div class="guide-meta-pills">
                                        <span class="guide-diff-badge"><c:out value="${a.difficulty}"/></span>
                                        <span class="guide-time-badge">
                                            <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                                            ${a.readingTime} phút
                                        </span>
                                    </div>
                                    <div class="guide-status-flags">
                                        <c:if test="${read[a.id]}">
                                            <span class="guide-flag-read" title="Đã học xong">
                                                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                                Đã đọc
                                            </span>
                                        </c:if>
                                        <c:if test="${saved[a.id]}">
                                            <span class="guide-flag-saved" title="Đã lưu vào danh sách">
                                                <svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path></svg>
                                            </span>
                                        </c:if>
                                    </div>
                                </div>

                                <h3 class="guide-article-title"><c:out value="${a.title}"/></h3>
                                <p class="guide-article-desc"><c:out value="${a.summary}"/></p>

                                <div class="guide-card-footer">
                                    <c:choose>
                                        <c:when test="${a.needsVerification}">
                                            <span class="guide-tag-verify">
                                                <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                                                Cần xác minh
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="guide-tag-verified">
                                                <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                                Nguồn xác thực
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                    <span class="guide-read-cta">
                                        Xem hướng dẫn
                                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                                    </span>
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
