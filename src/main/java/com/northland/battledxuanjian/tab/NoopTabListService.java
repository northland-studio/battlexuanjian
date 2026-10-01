package com.northland.battledxuanjian.tab;

/** 无 ProtocolLib 时的降级实现。 */
final class NoopTabListService implements TabListService {

    @Override
    public void refresh() {
        // 无需处理
    }

    @Override
    public void shutdown() {
        // 无需处理
    }
}
