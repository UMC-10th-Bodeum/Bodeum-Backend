UPDATE news
SET original_url = CASE source_name
    WHEN '순천시장애인종합복지관' THEN 'http://www.scrw.or.kr/'
    WHEN '구리시장애인종합복지관' THEN 'https://guriwel.or.kr/'
    WHEN '성남시장애인종합복지관' THEN 'https://www.rehab21.or.kr/'
    WHEN '부산광역시 금정구 장애인복지관' THEN 'https://www.gjrc.or.kr/'
    WHEN '사하구장애인종합복지관' THEN 'http://www.saharc.or.kr/'
    WHEN '진안군장애인종합복지관' THEN 'https://www.jinanrc.or.kr/'
    WHEN '영도구장애인복지관' THEN 'https://www.yeongdorc.or.kr/'
    WHEN '동구한마음종합복지관' THEN 'http://hanmaeum.org/'
    WHEN '합천군장애인복지센터' THEN 'https://www.assist.or.kr/'
    WHEN '사단법인 해솔' THEN 'http://haesols.modoo.at/'
    WHEN '광교아동발달센터' THEN 'http://xn--hc0bse75oc1af61a7vhkrhzwz.kr/'
    WHEN '꿈고래사회적협동조합' THEN 'https://dreamwhale.org/'
    WHEN '새길온사회적협동조합' THEN 'https://www.saegilon.co.kr/'
    WHEN '한국아동발달 사회적협동조합' THEN 'https://all-live.kr/'
    WHEN '해담심리언어발달센터' THEN 'http://www.haedamcenter.com/'
    WHEN '해담심리언어발달센터 2호점' THEN 'http://www.haedamcenter.com/'
    WHEN '아주청각언어센터' THEN 'https://hellosu26.wixsite.com/ajouhearing/blank-yur91'
    WHEN '우리아이통합발달센터' THEN 'https://uriai-center.com/'
    WHEN '우리아이통합발달센터 권선점' THEN 'https://uriai-center.com/'
    ELSE original_url
END
WHERE source_name IN (
    '순천시장애인종합복지관',
    '구리시장애인종합복지관',
    '성남시장애인종합복지관',
    '부산광역시 금정구 장애인복지관',
    '사하구장애인종합복지관',
    '진안군장애인종합복지관',
    '영도구장애인복지관',
    '동구한마음종합복지관',
    '합천군장애인복지센터',
    '사단법인 해솔',
    '광교아동발달센터',
    '꿈고래사회적협동조합',
    '새길온사회적협동조합',
    '한국아동발달 사회적협동조합',
    '해담심리언어발달센터',
    '해담심리언어발달센터 2호점',
    '아주청각언어센터',
    '우리아이통합발달센터',
    '우리아이통합발달센터 권선점'
);

UPDATE news
SET original_url = CASE title
    WHEN '장애공감문화' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=06&wnum=125'
    WHEN '자연체험활동' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34208'
    WHEN '동산골축제' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=02&wnum=34220'
    WHEN '내가그린에코' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=02&wnum=34283'
    WHEN '정보화교육' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34327'
    WHEN '지원고용' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34224'
    WHEN '근로지원인' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34360'
    WHEN '장애인일자리사업' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=02&wnum=34360'
    WHEN '장애인즐거운한마당' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=33633'
    WHEN '자조활동' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34176'
    WHEN '신비한 과학이야기' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=02&wnum=33737'
    WHEN '음악활동' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=33901'
    WHEN '여성장애인교육지원사업' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34371'
    WHEN '생생사진관' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34252'
    WHEN '한글배움터' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34316'
    WHEN '생활체육' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34317'
    WHEN '상상누림터' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34337'
    WHEN '동산오락실' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34323'
    WHEN '노리존' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=01&wnum=34184'
    WHEN '문화활동' THEN 'http://www.scrw.or.kr/bbs/view.php?wcode=02&wnum=33919'
    ELSE original_url
END
WHERE source_name = '순천시장애인종합복지관';

UPDATE news
SET original_url = 'https://www.gjrc.or.kr/SW_bbs/view.php?zipEncode=Zitm90wDU91DLLMDMqMBLrhDH91vt1drjrMCH9MyMetpSfMvWLME'
WHERE source_name = '부산광역시 금정구 장애인복지관'
  AND title = '외부연계프로그램 금토피아';
