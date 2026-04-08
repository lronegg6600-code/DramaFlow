INSERT INTO dramas (
    id, title, short_description, long_description, poster_url, cover_url, tags, region, language, publish_status, is_featured
) VALUES
    ('df-neon-vows', 'Neon Vows', 'A fake engagement turns into a power game inside a luxury tech empire.', 'Fast vertical drama with rapid reversals, premium hooks, and relationship leverage inside a public-facing technology dynasty.', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f', 'https://images.unsplash.com/photo-1515169067868-5387ec356754', '["Revenge","CEO","Romance"]', 'global', 'en', 'published', TRUE),
    ('df-last-heiress', 'The Last Heiress Signal', 'A banished daughter hijacks a media launch to expose a hidden inheritance war.', 'High-emotion return-from-exile drama built for fast discovery and feed conversion.', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1', 'https://images.unsplash.com/photo-1521119989659-a83eee488004', '["Family","Twist"]', 'global', 'en', 'published', TRUE),
    ('df-midnight-contract', 'Midnight Contract', 'An overnight marriage clause saves a company and ruins two guarded hearts.', 'Romance-forward short drama with premium back-half episodes.', 'https://images.unsplash.com/photo-1517841905240-472988babdf9', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d', '["CEO","Romance"]', 'global', 'en', 'published', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO episodes (
    id, drama_id, episode_no, title, description, duration_seconds, preview_seconds, is_premium, stream_key_placeholder, publish_status, sort_order
) VALUES
    ('df-neon-vows-e1', 'df-neon-vows', 1, 'Episode 1', 'A press event goes off-script.', 33, 0, FALSE, 'sample/df-neon-vows/e1', 'published', 1),
    ('df-neon-vows-e2', 'df-neon-vows', 2, 'Episode 2', 'A fake ring triggers a real scandal.', 33, 0, FALSE, 'sample/df-neon-vows/e2', 'published', 2),
    ('df-neon-vows-e3', 'df-neon-vows', 3, 'Episode 3', 'The contract leaks to the board.', 33, 0, FALSE, 'sample/df-neon-vows/e3', 'published', 3),
    ('df-neon-vows-e4', 'df-neon-vows', 4, 'Episode 4', 'The wedding clause activates.', 33, 15, TRUE, 'sample/df-neon-vows/e4', 'published', 4),
    ('df-neon-vows-e5', 'df-neon-vows', 5, 'Episode 5', 'A public betrayal changes the deal.', 33, 15, TRUE, 'sample/df-neon-vows/e5', 'published', 5),
    ('df-last-heiress-e1', 'df-last-heiress', 1, 'Episode 1', 'A return nobody expected.', 33, 0, FALSE, 'sample/df-last-heiress/e1', 'published', 1),
    ('df-last-heiress-e2', 'df-last-heiress', 2, 'Episode 2', 'The hidden clause comes out.', 33, 0, FALSE, 'sample/df-last-heiress/e2', 'published', 2),
    ('df-midnight-contract-e1', 'df-midnight-contract', 1, 'Episode 1', 'The marriage paper lands at midnight.', 33, 0, FALSE, 'sample/df-midnight-contract/e1', 'published', 1),
    ('df-midnight-contract-e2', 'df-midnight-contract', 2, 'Episode 2', 'A reputation clause arrives.', 33, 15, TRUE, 'sample/df-midnight-contract/e2', 'published', 2)
ON CONFLICT (id) DO NOTHING;
